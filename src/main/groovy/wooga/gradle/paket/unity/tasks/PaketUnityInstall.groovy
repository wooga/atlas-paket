/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.unity.tasks


import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import wooga.gradle.paket.base.utils.internal.PaketLock
import wooga.gradle.paket.base.utils.internal.PaketUPMWrapperReference
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences
import wooga.gradle.paket.unity.PaketUnityPlugin
import wooga.gradle.paket.unity.PaketUpmPackageSpec
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy
import wooga.gradle.paket.unity.internal.NugetToUpmPackageIdCache
import wooga.gradle.paket.unity.internal.UPMPackageDirectory

import java.nio.file.Paths

/**
 * A task to copy referenced NuGet packages into Unity3D projects.
 * <p>
 * This task will take as input a references and lock file to compute the files and directories to copy.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     task unityInstall(type:wooga.gradle.paket.unity.tasks.PaketUnityInstall) {*         referencesFile = file('paket.unity3D.references')
 *         lockFile = file('../paket.lock')
 *         frameworks = ["net11", "net20", "net35"]
 *         paketOutputDirectoryName = "PaketUnity3D"
 *}*}
 * </pre>
 */
class PaketUnityInstall extends ConventionTask implements PaketUpmPackageSpec {
    static final String PACKAGE_JSON = "package.json"

    /**
     * @return the path to a {@code paket.unity3d.references} file
     */
    @InputFile
    File referencesFile

    /**
     * @return the path to a {@code paket.lock} file
     */
    @InputFile
    File lockFile

    /**
     * @return a list of .NET framework identifiers.
     */
    @Input
    List<String> frameworks

    /**
     * The name of the output directory with the unity projects {@code Assets} folder.
     */
    @Input
    String paketOutputDirectoryName

    /**
     * Whether assembly definition files (.asmdef) should be included during installation
     */
    @Input
    Boolean includeAssemblyDefinitions = false

    /**
     * List of upm package id's in the outputs folder that we should exclude from deleting on non-incremental cleanup.
     */
    @Input
    @Optional
    List<String> preInstalledUpmPackages

    @Input
    AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy

    /**
     * We need this cache, since the mapping from nuget to upm package Id exists only in the package.json of a package from the paket packages cache
     * since this can get deleted, we need to look inside the not-yet-deleted unity upm package and deduce the nuget & paket Id from there.
     */
    @Internal
    NugetToUpmPackageIdCache nugetToUpmCache

    public final static String assemblyDefinitionFileExtension = "asmdef"

    /**
     * @return The installation output directory
     */
    @OutputDirectory
    File getOutputDirectory() {
        // TODO: Update. How the heck does this work nowadays when the Unity project directory is at the root??
        new File(referencesFile.parentFile, "Assets/${getPaketOutputDirectoryName()}")
    }

    @Internal
    @Deprecated
    String getPackagesDirectory() {
        return paketPackagesDirectory.canonicalPath
    }

    /**
     * @return The path to where paket extracts the downloaded packages
     */
    @Internal
    File getPaketPackagesDirectory() {
        // The MacOS Java implementation resolves 'Packages' and 'packages' in separate, as if it was on a unix system.
        // However, MacOS actually treat those two folders as the same, so we use .canonicalPath to make sure that we get the
        // correct capitalization on the existing folder.
        def normalizedPath = new File(project.projectDir, "packages").canonicalPath
        return new File(normalizedPath)
    }

    /**
     * @return the files to install into the Unity3D project.
     */
    @InputFiles
    FileCollection getInputFiles() {
        Set<File> files = []
        def references = new PaketUnityReferences(getReferencesFile())

        if (!getLockFile().exists()) {
            return null
        }

        def locks = new PaketLock(getLockFile())
        def dependencies = locks.getAllDependencies(references.nugets)
        dependencies.each { nuget ->
            if (!PaketUnwrapUPMPackages.isUPMWrapper(nuget, project)) {
                def depFiles = getFilesForPackage(nuget)
                files << depFiles
            }
        }

        project.files(files)
    }

    PaketUnityInstall() {
        description = 'Copy paket dependencies into unity projects'
        group = PaketUnityPlugin.GROUP

        this.doLast {
            if (isPaketUpmPackageEnabled().get()) {
                def references = new PaketUnityReferences(getReferencesFile())
                def locks = new PaketLock(getLockFile())
                def packages = []

                locks.getAllDependencies(references.nugets).each { nugetId ->
                    if (!PaketUnwrapUPMPackages.isUPMWrapper(nugetId, project) && nugetToUpmCache.containsKey(nugetId)) {
                        packages << [nugetId, new File(outputDirectory, nugetToUpmCache.getUpmId(nugetId))]
                    }
                }

                packages.each {
                    createPackageManifestIfNotExists(it[0], it[1])
                }
            }
        }
    }

    /**
     * @param nugetId The name of the package
     * @return The files to be copied over from the package with the given id
     */
    Set<File> getFilesForPackage(String nugetId) {
        def packagesDirectory = paketPackagesDirectory;
        def fileTree = project.fileTree(packagesDirectory)
        fileTree.include("${nugetId}/content/**")

        getFrameworks().each({
            fileTree.include("${nugetId}/lib/${it}/**")
        })

        fileTree.include("${nugetId}/lib/*.dll")

        fileTree.exclude("**/*.pdb")
        fileTree.exclude("**/Meta")

        if (!getIncludeAssemblyDefinitions()) {
            fileTree.exclude("**/*.${assemblyDefinitionFileExtension}")
        }

        def files = fileTree.files
        return files
    }

    /**
     * Executes the copy of the packages downloaded by paket
     * into the appropriate Unity project's package directory.
     * @param inputs The input files
     */
    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {
        logger.quiet("include libs with frameworks: " + getFrameworks().join(", "))
        if (isPaketUpmPackageEnabled().get()) {
            logger.info("Update Nuget2Upm PackageId Cache")
            nugetToUpmCache = new NugetToUpmPackageIdCache(project,
                paketPackagesDirectory,
                inputFiles,
                outputDirectory,
                paketUpmPackageManifests,
                defaultNamespace.get())

            nugetToUpmCache.dumpCacheToLog()
        }

        if (!inputs.incremental) {
            if (getOutputDirectory().exists()) {
                cleanOutputDirectory()
            }
        }

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails outOfDate) {
                if (inputFiles.contains(outOfDate.file)) {
                    // Compose the path where the package file should be copied to
                    def outputPath = transformInputToOutputPath(outOfDate.file, paketPackagesDirectory)
                    logger.info("${outOfDate.added ? "install" : "update"}: ${outputPath}")
                    FileUtils.copyFile(outOfDate.file, outputPath)
                    assert outputPath.exists()
                }
            }
        })

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails removed) {
                logger.info("remove: ${removed.file}")
                removed.file.delete()

                // Delete the files that were copied over from the paket packages
                def outputPath = transformInputToOutputPath(removed.file, paketPackagesDirectory)
                outputPath.delete()

                // Delete generated package.jsons
                if (isPaketUpmPackageEnabled().get()) {
                    def relativePath = paketPackagesDirectory.toURI().relativize(removed.file.toURI()).getPath()
                    def paketId = relativePath.split("/").toList()[0]
                    def packageJson = Paths.get(getOutputDirectory().absolutePath, nugetToUpmCache.getUpmId(paketId), PACKAGE_JSON).toFile()
                    def packageJsonMeta = Paths.get(getOutputDirectory().absolutePath, nugetToUpmCache.getUpmId(paketId), "${PACKAGE_JSON}.meta").toFile()
                    if (packageJson.exists()) packageJson.delete()
                    if (packageJsonMeta.exists()) packageJsonMeta.delete()
                }

                File parent = outputPath.parentFile
                while (parent.isDirectory() && parent.listFiles().toList().empty) {
                    logger.info("Garbage collecting: ${removed.file}")
                    parent.deleteDir()
                    parent = parent.parentFile
                }

                assert removed.removed
                assert !outputPath.exists()
            }
        })
    }

    protected void createPackageManifestIfNotExists(String nugetId, File packageDir) {

        def upmPackage = new UPMPackageDirectory(packageDir)
        if (upmPackage.exists() && !upmPackage.manifestFile.present) {

            // Check for overrides that were assigned on an extension > task level.
            // These overrides will be used whenever there's a match for a given nuget id
            def overrides = paketUpmPackageManifests.getting(upmPackage.name).getOrElse([:])
            // Generate the default manifest contents
            def packageManifest = nugetToUpmCache.generateManifest(nugetId, overrides)

            upmPackage.writeManifest(packageManifest)

            logger.info("generated package.json (${packageManifest['name']}) for $packageDir")
        }
    }

    protected void cleanOutputDirectory() {
        def tree = project.fileTree(getOutputDirectory())

        logger.info("Delete files in directory: ${getOutputDirectory()}")

        // If the strategy is manual, do not delete asmdefs
        if (getAssemblyDefinitionFileStrategy() == AssemblyDefinitionFileStrategy.manual) {
            tree.exclude("**/*.asmdef")
            tree.exclude("**/*.asmdef.meta")
        }
        if (isPaketUpmPackageEnabled().get()) {
            def upmPackageDirs = []
            project.file(getOutputDirectory()).eachDir {
                if (!(it.name in preInstalledUpmPackages) && new File(it, PACKAGE_JSON).exists()) {
                    upmPackageDirs << it
                }
            }
            upmPackageDirs.each { it.deleteDir() }
        } else {
            project.delete(tree)
        }

        def emptyDirs = []
        project.fileTree(getOutputDirectory()).visit(new FileVisitor() {
            @Override
            void visitDir(FileVisitDetails dirDetails) {
                File f = dirDetails.file
                def children = project.fileTree(f).filter { it.isFile() }.files
                if (children.size() == 0) {
                    emptyDirs << f
                }
            }

            @Override
            void visitFile(FileVisitDetails fileDetails) {
            }
        })
        emptyDirs.reverseEach { it.delete() }
    }

    /**
     * @param inputFile A file from an extracted nuget package directory
     * @param paketDirectory The base directory for the file. It it used to extract a relative folder structure.
     * @return A file mapped to the output directory, which keeps the same relative directory structure
     */
    private File transformInputToOutputPath(File inputFile, File paketDirectory) {
        def relativePath = paketDirectory.toURI().relativize(inputFile.toURI()).path
        def pathSegments = relativePath.split('/').toList()
        // Remove the intermediary paket "content" folder
        pathSegments.remove(1)

        if (isPaketUpmPackageEnabled().get()) {
            def nugetId = pathSegments.remove(0)
            if (inputFile.name in [PACKAGE_JSON, "${PACKAGE_JSON}.meta"]) {
                return Paths.get(getOutputDirectory().absolutePath, nugetToUpmCache.getUpmId(nugetId), inputFile.name).toFile()
            }
            // Replace the nugetId with upmId
            pathSegments.add(0, nugetToUpmCache.getUpmId(nugetId))
            return new File(getOutputDirectory(), pathSegments.join(File.separator))
        }

        def file = new File(getOutputDirectory(), pathSegments.join(File.separator))
        return file
    }
}

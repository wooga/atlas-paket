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
import wooga.gradle.paket.unity.internal.UPMPaketPackage

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

    // We need this cache, since the mapping from nuget to upm package Id exists only in the package.json of a package from the paket packages cache
    // since this can get deleted, we need to look inside the not-yet-deleted unity upm package and deduce the nuget & paket Id from there.
    @Internal
    NugetToUpmPackageIdCache nugetToUpmId

    public final static String assemblyDefinitionFileExtension = "asmdef"
    public final static String assemblyReferenceFileExtension = "asmref"

    /**
     * @return the installation output directory
     */
    @OutputDirectory
    File getOutputDirectory() {
        new File(getReferencesFile().getParentFile(), "Assets/${getPaketOutputDirectoryName()}")
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

                locks.getAllDependencies(references.nugets).each { nuget ->
                    if (!PaketUnwrapUPMPackages.isUPMWrapper(nuget, project) && nugetToUpmId.containsKey(nuget)) {
                        packages << [nuget, new File(outputDirectory, nugetToUpmId.getUpmId(nuget))]
                    }
                }

                packages.each {
                    createPackageManifestIfNotExists(it[0], it[1])
                }
            }
        }
    }

    /**
     * @param nuget The name of the package
     * @return The files to be copied over from this package
     */
    Set<File> getFilesForPackage(String nuget) {
        def packagesDirectory = getPackagesDirectory();
        def fileTree = project.fileTree(dir: project.projectDir)
        fileTree.include("${packagesDirectory}/${nuget}/content/**")

        getFrameworks().each({
            fileTree.include("${packagesDirectory}/${nuget}/lib/${it}/**")
        })

        fileTree.include("${packagesDirectory}/${nuget}/lib/*.dll")

        fileTree.exclude("**/*.pdb")
        fileTree.exclude("**/Meta")

        if (!getIncludeAssemblyDefinitions()) {
            fileTree.exclude("**/*.${assemblyDefinitionFileExtension}")
            fileTree.exclude("**/*.${assemblyDefinitionFileExtension}.meta")
            fileTree.exclude("**/*.${assemblyReferenceFileExtension}")
            fileTree.exclude("**/*.${assemblyReferenceFileExtension}.meta")
        }

        def files = fileTree.files
        return files
    }

    @Input
    String getPackagesDirectory() {
        PaketUPMWrapperReference.getPackagesDirectory(project)
    }

    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {
        logger.quiet("include libs with frameworks: " + getFrameworks().join(", "))
        if (isPaketUpmPackageEnabled().get()) {
            logger.info("Update Nuget2Upm PackageId Cache")
            nugetToUpmId = new NugetToUpmPackageIdCache(project, inputFiles, outputDirectory, paketUpmPackageManifests)
            nugetToUpmId.dumpCacheToLog()
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
                    def outputPath = transformInputToOutputPath(outOfDate.file, project.file(getPackagesDirectory()))
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
                def outputPath = transformInputToOutputPath(removed.file, project.file(getPackagesDirectory()))
                outputPath.delete()

                // delete generated package.jsons
                if (isPaketUpmPackageEnabled().get()) {
                    def relativePath = project.file(getPackagesDirectory()).toURI().relativize(removed.file.toURI()).getPath()
                    def paketId = relativePath.split("/").toList()[0]
                    def packageJson = Paths.get(getOutputDirectory().absolutePath, nugetToUpmId.getUpmId(paketId), PACKAGE_JSON).toFile()
                    def packageJsonMeta = Paths.get(getOutputDirectory().absolutePath, nugetToUpmId.getUpmId(paketId), "${PACKAGE_JSON}.meta").toFile()
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
        def upmPaket = new UPMPaketPackage(packageDir)
        if(packageDir.exists() && !upmPaket.packageManifest.present) {

            def pkgJsonOverrides = paketUpmPackageManifests.getting(upmPaket.name).getOrElse([:])
            def pkgJson = UPMPaketPackage.basicUPMPackageManifest(nugetToUpmId.getUpmId(nugetId), nugetId, pkgJsonOverrides)

            upmPaket.writePackageManifest(pkgJson)
            logger.info("generated package.json (${pkgJson['name']}) for $packageDir")
        }
    }

    protected void cleanOutputDirectory() {
        def tree = project.fileTree(getOutputDirectory())

        logger.info("delete files in directory: ${getOutputDirectory()}")

        // If the strategy is manual, do not delete asmdefs
        if (getAssemblyDefinitionFileStrategy() == AssemblyDefinitionFileStrategy.manual) {
            tree.exclude("**/*.asmdef")
            tree.exclude("**/*.asmdef.meta")
        }
        if(isPaketUpmPackageEnabled().get()) {
            def upmPackageDirs = []
            project.file(getOutputDirectory()).eachDir {
                if ( !(it.name in preInstalledUpmPackages) && new File(it, PACKAGE_JSON).exists()) {
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

    private File transformInputToOutputPath(File inputFile, File baseDirectory) {
        def relativePath = baseDirectory.toURI().relativize(inputFile.toURI()).getPath()
        def pathSegments = relativePath.split("/").toList()
        // removes the intermediary paket "content" folder
        pathSegments.remove(1)

        if (isPaketUpmPackageEnabled().get()) {
            def paketId = pathSegments.remove(0)
            if(inputFile.name in [PACKAGE_JSON, "${PACKAGE_JSON}.meta"]) {
                return Paths.get(getOutputDirectory().absolutePath, nugetToUpmId.getUpmId(paketId), inputFile.name).toFile()
            }
            // replace the paketId with upmId
            pathSegments.add(0, nugetToUpmId.getUpmId(paketId))
            return new File(getOutputDirectory(), pathSegments.join(File.separator))
        }

        return new File(getOutputDirectory(), pathSegments.join(File.separator))
    }
}

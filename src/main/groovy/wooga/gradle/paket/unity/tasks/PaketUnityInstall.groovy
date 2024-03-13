/*
 * Copyright 2018-2024 Wooga GmbH
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
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import wooga.gradle.paket.base.utils.internal.PaketLock
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences
import wooga.gradle.paket.unity.PaketUnityPlugin
import wooga.gradle.paket.unity.PaketUpmPackageSpec
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
 *}*}
 * </pre>
 */
class PaketUnityInstall extends AbstractPaketUnityTask implements PaketUpmPackageSpec {

    /**
     * @return a list of .NET framework identifiers.
     */
    @Input
    List<String> frameworks

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

    /**
     * We need this cache, since the mapping from nuget to upm package Id exists only in the package.json of a package from the paket packages cache
     * since this can get deleted, we need to look inside the not-yet-deleted unity upm package and deduce the nuget & paket Id from there.
     */
    @Internal
    NugetToUpmPackageIdCache nugetToUpmCache

    @Override
    File getOutputDirectory() {
        if (paketUpmPackageEnabled.get()){
            return new File(referencesFile.parentFile, "Packages")
        }
        new File(referencesFile.parentFile, "Assets/Paket.Unity3D")
    }

    /**
     * @return the files to install into the Unity3D project.
     */
    @InputFiles
    FileCollection getInputFiles() {
        collectInputFiles(nugetId -> {

            if (isUPMWrapper(nugetId)){
                return new HashSet<File>()
            }

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
        })
    }

    PaketUnityInstall() {
        description = 'Copy paket dependencies into unity projects'
        group = PaketUnityPlugin.GROUP

        this.doLast {
            if (paketUpmPackageEnabled.get()) {
                def references = new PaketUnityReferences(referencesFile)
                def locks = new PaketLock(lockFile)
                def packages = []

                locks.getAllDependencies(references.nugets).each { nugetId ->
                    if (!isUPMWrapper(nugetId) && nugetToUpmCache.containsKey(nugetId)) {
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
     * Executes the copy of the packages downloaded by paket
     * into the appropriate Unity project's package directory.
     * @param inputs The input files
     */
    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {
        logger.quiet("> Now performing copy of packages from paket to Unity project. Will include libs with frameworks: " + getFrameworks().join(", "))

        if (paketUpmPackageEnabled.get()) {
            logger.info("> Will setup paket packages alongside the UPM packages")
            nugetToUpmCache = new NugetToUpmPackageIdCache(project,
                paketPackagesDirectory,
                inputFiles,
                outputDirectory,
                paketUpmPackageManifests,
                defaultUpmNamespace.get())

            nugetToUpmCache.dumpCacheToLog()
        }

        if (!inputs.incremental) {
            if (outputDirectory.exists()) {
                cleanOutputDirectory()
            }
        }

        // INSTALL/UPDATE
        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails outOfDate) {
                if (inputFiles.contains(outOfDate.file)) {
                    // Compose the path where the package file should be copied to
                    def outputPath = transformInputToOutputPath(outOfDate.file)
                    logger.info("${outOfDate.added ? "+ INSTALL" : "+= UPDATE"}: ${outputPath}")
                    FileUtils.copyFile(outOfDate.file, outputPath)
                    assert outputPath.exists()
                }
            }
        })

        // REMOVE
        inputs.removed(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails removed) {
                logger.info("- REMOVED: ${removed.file}")
                removed.file.delete()

                // Delete the files that were copied over from the paket packages
                def outputPath = transformInputToOutputPath(removed.file)
                outputPath.delete()

                // Delete generated package.jsons
                if (paketUpmPackageEnabled.get()) {
                    def relativePath = paketPackagesDirectory.toURI().relativize(removed.file.toURI()).getPath()
                    def nugetId = relativePath.split("/").toList()[0]
                    def packageManifestFile = Paths.get(outputDirectory.absolutePath, nugetToUpmCache.getUpmId(nugetId), packageManifestFileName).toFile()
                    def packageManifestMetaFile = Paths.get(outputDirectory.absolutePath, nugetToUpmCache.getUpmId(nugetId), "${packageManifestFileName}.meta").toFile()
                    if (packageManifestFile.exists()) packageManifestFile.delete()
                    if (packageManifestMetaFile.exists()) packageManifestMetaFile.delete()
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

    /**
     * Cleans the output directory of both output package files
     * and potentially assembly definition files
     */
    protected void cleanOutputDirectory() {

        // THIS IS A PROBLEM IF THE PACKAGES DIRECTORY IS BOTH
        // FOR PAKET AND UNITY PROJECT PACKAGES.
        logger.info("> Now cleaning output directories")

        // UPM MODE: If we installed paket packages onto the unity packages' directory
        if (paketUpmPackageEnabled.get()) {
            def upmPackageDirs = []
            project.file(outputDirectory).eachDir {
                if (!(it.name in preInstalledUpmPackages) && new File(it, packageManifestFileName).exists()) {
                    upmPackageDirs << it
                }
            }
            upmPackageDirs.each { it.deleteDir() }
        }
        // PAKET-ONLY LEGACY MODE: Delete the paket directory
        else {
            def tree = project.fileTree(outputDirectory)
            project.delete(tree.files)
        }

        def emptyDirs = []
        project.fileTree(outputDirectory).visit(new FileVisitor() {
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
    private File transformInputToOutputPath(File inputFile) {

        // We get the relative file structure from where the file was in the paket directory
        inputFile = new File(inputFile.canonicalPath)
        def relativePath = paketPackagesDirectory.toURI().relativize(inputFile.toURI()).path

        //  Also remove the intermediary paket "content" folder
        def pathSegments = relativePath.split('/').toList()
        pathSegments.remove(1)

        if (paketUpmPackageEnabled.get()) {
            def nugetId = pathSegments.remove(0)
            if (inputFile.name in [packageManifestFileName, "${packageManifestFileName}.meta"]) {
                return Paths.get(outputDirectory.absolutePath, nugetToUpmCache.getUpmId(nugetId), inputFile.name).toFile()
            }
            // Replace the nugetId with upmId
            pathSegments.add(0, nugetToUpmCache.getUpmId(nugetId))
            return new File(outputDirectory, pathSegments.join(File.separator))
        }

        def file = new File(outputDirectory, pathSegments.join(File.separator))
        return file
    }
}

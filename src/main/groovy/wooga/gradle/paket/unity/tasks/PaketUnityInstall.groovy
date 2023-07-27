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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import wooga.gradle.paket.base.utils.internal.PaketLock
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences
import wooga.gradle.paket.unity.PaketUnityPlugin
import wooga.gradle.paket.unity.PaketUpmPackageSpec
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy
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

    @Input
    AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy

    public final static String assemblyDefinitionFileExtension = "asmdef"

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
                def packages = locks.getAllDependencies(references.nugets).collect {
                    new File(outputDirectory, it)
                }
                packages.each { createPackageManifestIfNotExists(it) }
            }
        }
    }

    /**
     * @param nuget The name of the package
     * @return The files to be copied over from this package
     */
    Set<File> getFilesForPackage(String nuget) {
        def fileTree = project.fileTree(dir: project.projectDir)
        fileTree.include("packages/${nuget}/content/**")

        getFrameworks().each({
            fileTree.include("packages/${nuget}/lib/${it}/**")
        })

        fileTree.include("packages/${nuget}/lib/*.dll")

        fileTree.exclude("**/*.pdb")
        fileTree.exclude("**/Meta")

        if (!getIncludeAssemblyDefinitions()) {
            fileTree.exclude("**/*.${assemblyDefinitionFileExtension}")
        }

        def files = fileTree.files
        return files
    }

    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {

        logger.quiet("include libs with frameworks: " + getFrameworks().join(", "))

        if (!inputs.incremental) {
            if (getOutputDirectory().exists()) {
                cleanOutputDirectory()
            }
        }

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails outOfDate) {
                if (inputFiles.contains(outOfDate.file)) {
                    def outputPath = transformInputToOutputPath(outOfDate.file, project.file("packages"))
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
                def outputPath = transformInputToOutputPath(removed.file, project.file("packages"))
                outputPath.delete()

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

    protected void createPackageManifestIfNotExists(File packageDir) {
        def upmPaket = new UPMPaketPackage(packageDir)
        if(packageDir.exists() && !upmPaket.packageManifest.present) {

            def pkgJsonOverrides = paketUpmPackageManifests.getting(upmPaket.name).getOrElse([:])
            def pkgJson = UPMPaketPackage.basicUPMPackageManifest("com.wooga.nuget.${upmPaket.name.toLowerCase()}", pkgJsonOverrides)

            upmPaket.writePackageManifest(pkgJson)
            logger.info("generated package.json (${pkgJson['name']}) for $packageDir")
        }
    }

    protected void cleanOutputDirectory() {
        def tree = project.fileTree(getOutputDirectory())

        // If the strategy is manual, do not delete asmdefs
        if (getAssemblyDefinitionFileStrategy() == AssemblyDefinitionFileStrategy.manual) {
            tree.exclude("**/*.asmdef")
            tree.exclude("**/*.asmdef.meta")
        }
        if(isPaketUpmPackageEnabled().get()) {
            tree.exclude("manifest.json")
        }

        logger.info("delete files in directory: ${getOutputDirectory()}")
        project.delete(tree)

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
        pathSegments.remove(1)
        if(isPaketUpmPackageEnabled().get() && inputFile.name in ["package.json", "package.json.meta"]) {
            return Paths.get(getOutputDirectory().absolutePath, pathSegments[0], inputFile.name).toFile()
        }
        return new File(getOutputDirectory(), pathSegments.join(File.separator))
    }
}

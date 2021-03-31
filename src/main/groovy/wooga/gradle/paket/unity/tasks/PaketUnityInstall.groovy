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

import groovy.transform.Internal
import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileTree
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
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy

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
class PaketUnityInstall extends ConventionTask {

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
     * Whether test assemblies (in $PACKAGE/Tests) should be included during installation
     */
    @Input
    Boolean includeTests = false

    @Input
    AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy

    public final static String assemblyDefinitionFileExtension = "asmdef"
    public final static String testsDirectoryName = "Tests"

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


        // By convention, the root of the Unity project will have a
        // paket.references file that we use to decide what files from the packages we will copy
        // into the project
        def references = new PaketUnityReferences(getReferencesFile())

        // If there's no lock file, do nothing
        if (!getLockFile().exists()) {
            return null
        }

        // Retrieve all dependencies by using the lock file,
        // filtering them and giving only unique entries
        def locks = new PaketLock(getLockFile())
        def dependencies = locks.getAllDependencies(references.referenceNames)
        dependencies.each { nuget ->

            if (!PaketUnwrapUPMPackages.isUPMWrapper(nuget, project)) {

                // An optional action to invoke while packaging files
                Action<? super ConfigurableFileTree> onPackage = null
                // TODO: Include logging for when tests/assemblies are included

                // If it's an unity wdk package, we have additional properties for it
                // which we can use to further filter the files
                if (references.containsReference(nuget)) {

                    def ref = references.getReference(nuget)
                    onPackage = { fileTree ->

                        // TESTS
                        if (!ref.includeTests && !getIncludeTests()) {
                            // Exclude the Tests directory on the second level of the project,
                            // since wdks conventionally have the layout:
                            // $WDK/Runtime
                            // $WDK/Editor
                            // $WDK/Tests
                            fileTree.exclude("**/${nuget}/content/*/${testsDirectoryName}/**/*")
                        }
                        else{
                            logger.info("Including tests from ${nuget}")
                        }

                        // ASSEMBLY DEFINITION FILES
                        if (!ref.includeAssemblies && !getIncludeAssemblyDefinitions()) {
                            // Exclude all assembly definition files from the project
                            fileTree.exclude("**/${nuget}/content/*.${assemblyDefinitionFileExtension}")
                        }
                        else{
                            logger.info("Including assembly definition files from ${nuget}")
                        }
                    }
                }

                def packageFiles = getFilesForPackage(nuget, onPackage)
                    files << packageFiles
            }
        }

        project.files(files)
    }

    /**
     * @param nuget The name of the nuget package's directory
     * @return Given the name of a (downloaded) nuget package, returns the path to all
     * the applicable files from it that should be copied to the project
     */
    Set<File> getFilesForPackage(String nuget, Action<? super ConfigurableFileTree> action = null) {

        logger.info("Including files from nuget package: ${nuget}")
        def fileTree = project.fileTree(dir: project.projectDir)
        fileTree.include("packages/${nuget}/content/**")

        getFrameworks().each({
            fileTree.include("packages/${nuget}/lib/${it}/**")
        })

        fileTree.include("packages/${nuget}/lib/*.dll")

        fileTree.exclude("**/*.pdb")
        fileTree.exclude("**/Meta")

        if (action != null) {
            action.execute(fileTree)
        }

        def files = fileTree.files
        return files
    }

    /**
     * Copies all the resolved nuget package files from their source directories
     * onto the project
     */
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

    protected void cleanOutputDirectory() {
        def tree = project.fileTree(getOutputDirectory())

        if(getAssemblyDefinitionFileStrategy() == AssemblyDefinitionFileStrategy.manual) {
            tree.exclude("**/*.asmdef")
            tree.exclude("**/*.asmdef.meta")
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
        def outputPath = new File(getOutputDirectory(), pathSegments.join(File.separator))
        outputPath
    }
}

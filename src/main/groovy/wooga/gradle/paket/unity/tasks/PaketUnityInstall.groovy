/*
 * Copyright 2017 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import wooga.gradle.paket.base.utils.internal.PaketLock
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences
import wooga.gradle.paket.unity.PaketUnityPlugin

class PaketUnityInstall extends ConventionTask {

    @Input
    File referencesFile

    @Input
    File lockFile

    @Input
    String paketOutputDirectoryName

    File projectRoot

    @OutputDirectory
    File getOutputDirectory() {
        new File(projectRoot, "Assets/${getPaketOutputDirectoryName()}")
    }

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
            files << getFilesForPackage(nuget)
        }
        project.files(files)
    }

    Set<File> getFilesForPackage(String nuget) {
        Set<File> files = []
        files << detectContentFiles(nuget)
        files << detectContentDLLs(nuget)
        files
    }

    Set<File> detectContentFiles(String nuget) {
        def fileTree = project.fileTree(dir: project.projectDir)
        fileTree.include("packages/${nuget}/lib/net35/**")
        fileTree.exclude("**/*.meta")
        fileTree.exclude("**/Meta")
        fileTree.files
    }

    Set<File> detectContentDLLs(String nuget) {
        def fileTree = project.fileTree(dir: project.projectDir)
        fileTree.include("packages/${nuget}/content/**")
        fileTree.exclude("**/*.meta")
        fileTree.exclude("**/Meta")
        fileTree.files
    }

    PaketUnityInstall() {
        description = 'Copy paket dependencies into unity projects'
        group = PaketUnityPlugin.GROUP
    }

    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            if (getOutputDirectory().exists()) {
                getOutputDirectory().deleteDir()
                assert !getOutputDirectory().exists()
            }
        }

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails outOfDate) {
                def outputPath = transformInputToOutputPath(outOfDate.file, project.file("packages"))
                println("copy: ${outputPath}")
                FileUtils.copyFile(outOfDate.file, outputPath)
                assert outputPath.exists()
            }
        })

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails removed) {
                println("remove: ${removed.file}")
                removed.file.delete()
                def outputPath = transformInputToOutputPath(removed.file, project.file("packages"))
                outputPath.delete()

                File parent = outputPath.parentFile
                while (parent.isDirectory() && parent.listFiles().toList().empty) {
                    parent.deleteDir()
                    parent = parent.parentFile
                }

                assert removed.removed
                assert !outputPath.exists()
            }
        })
    }

    File transformInputToOutputPath(File inputFile, File baseDirectory) {
        def relativePath = baseDirectory.toURI().relativize(inputFile.toURI()).getPath()
        def pathSegments = relativePath.split(File.separator).toList()
        pathSegments.remove(1)
        def outputPath = new File(getOutputDirectory(), pathSegments.join(File.separator))
        outputPath
    }
}

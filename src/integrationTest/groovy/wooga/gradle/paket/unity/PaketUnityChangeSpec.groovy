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

package wooga.gradle.paket.unity

import nebula.test.IntegrationSpec
import spock.lang.Unroll
import wooga.gradle.extensions.PaketDependency
import wooga.gradle.extensions.PaketDependencySetup
import wooga.gradle.extensions.PaketUnity
import wooga.gradle.extensions.PaketUnitySetup
import wooga.gradle.paket.get.PaketGetPlugin

class PaketUnityChangeSpec extends IntegrationSpec {

    final static String STD_OUT_ALL_OUT_OF_DATE = "All input files are considered out-of-date for incremental task"

    @PaketDependency(projectDependencies = ["D1", "D2", "D3"])
    PaketDependencySetup paketSetup

    @PaketUnity(projectReferences = ["D1", "D2", "D3"])
    PaketUnitySetup unityProject1

    @PaketUnity(projectReferences = ["D2", "D4"])
    PaketUnitySetup unityProject2

    @PaketUnity(projectReferences = ["D3", "D4"])
    PaketUnitySetup unityProject3


    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Unroll
    def "task :paketUnityInstall when #message was up to date #wasUpToDate"() {
        given: "a root project with a unity project"

        and: "paket dependency file"
        paketSetup.createDependencies(rootDependencies)

        and: "unity project #unityProjectName with references #projectReferences"
        unityProject1.createOrUpdateReferenceFile(projectReferences)

        and: "paketUnityInstall is executed"
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        and:
        unityProject1.createOrUpdateReferenceFile(projectReferenceUpdate)

        when: "paketUnityInstall is executed again"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME) == wasUpToDate

        unityProject1.projectReferencesFile.exists()

        appliedReferencesAfterUpdate.every { ref ->
            new File(unityProject1.installDirectory, ref as String).exists()
        }

        where:
        rootDependencies   | projectReferences | projectReferenceUpdate | wasUpToDate | message
        ["D1"]             | ["D1"]            | []                     | false       | "change remove all"
        ["D1"]             | ["D1"]            | ["D1"]                 | true        | "mime time change"
        ["D1", "D2"]       | ["D1"]            | ["D2"]                 | false       | "change remove one dependency"
        ["D1", "D2"]       | ["D1", "D2"]      | ["D2"]                 | false       | "change remove one dependency"
        ["D1", "D2"]       | ["D1", "D2"]      | ["D1", "D2"]           | true        | "mime time change"
        ["D1", "D2"]       | ["D1", "D2"]      | []                     | false       | "change remove all multiple"
        ["D1", "D2", "D3"] | ["D3"]            | ["D1", "D2", "D4"]     | false       | "remove and change and ignore not available"

        appliedReferences = rootDependencies.intersect(projectReferences)
        appliedReferencesAfterUpdate = rootDependencies.intersect(projectReferenceUpdate)
    }

    def "run paketUnityInstall a root project with multiple unity projects"() {
        when: "paketUnityInstall is executed"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)

        unityProject1.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject1.installDirectory, ref as String).exists()
        }

        unityProject2.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject2.installDirectory, ref as String).exists()
        }

        unityProject3.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject3.installDirectory, ref as String).exists()
        }
    }

    def "task :paketInstall is incremental"() {
        given: "a root project with a unity project"
        and: "paket dependency file"
        paketSetup.createDependencies(["D1", "D2"])
        def dep1 = createFile("packages/D1/content/ContentFile.cs")
        def dep2 = createFile("packages/D2/content/ContentFile.cs")

        and: "unity project #unityProjectName with references #projectReferences"
        unityProject1.createOrUpdateReferenceFile(["D1", "D2"])

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        when: "change source file in dependencies"
        dep1 << "change"

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        containsHasChangedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)
    }

    def "task :paketInstall removes incrementally with changed source"() {
        given: "a root project with a unity project"
        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        dep1.delete()

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        containsHasRemovedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        !out1.exists()
        out2.exists()
    }

    def "task :paketInstall adds with change in target directory"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        out2.delete()

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        allFilesOutOfDate(result.standardOutput)

        out1.exists()
        out2.exists()
    }

    def "task :paketInstall adds incrementally with changed target content"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        out2 << "local patch"

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        allFilesOutOfDate(result.standardOutput)

        out1.exists()
        out2.exists()
    }

    @Unroll
    def "task :paketInstall keeps files with #filePattern in #location paket install directory"() {
        given: "a file matching the file pattern"
        def baseDir = (location == "root") ? unityProject1.installDirectory : new File(unityProject1.installDirectory, "some/nested/directory")
        baseDir.mkdirs()
        def fileToKeep = createFile("test${filePattern}", baseDir) << "random content"

        when:
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        fileToKeep.exists()

        where:
        filePattern    | location
        ".asmdef"      | "root"
        ".asmdef"      | "nested"
        ".asmdef.meta" | "root"
        ".asmdef.meta" | "nested"
    }

    def "task :paketInstall deletes empty directories"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def paketDir = unityProject1.installDirectory

        and: "some empty directories in output directory"
        def rootDir = new File(paketDir, "dirAtRoot")
        def secondLevel = new File(rootDir, "dirAtSecondLevel")
        def thirdLevel = new File(secondLevel, "dirAtThirdLevel")
        thirdLevel.mkdirs()

        when:
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        !rootDir.exists()
        !secondLevel.exists()
        !thirdLevel.exists()
    }

    def containsHasChangedOrDeletedOutput(String stdOut, String filePath) {
        containsHasChangedOutput(stdOut, filePath) || containsHasRemovedOutput(stdOut, filePath)
    }

    def containsHasChangedOutput(String stdOut, String filePath) {
        stdOut.contains("inputFiles' file ${filePath} has changed.")
    }

    def containsHasRemovedOutput(String stdOut, String filePath) {
        stdOut.contains("inputFiles' file ${filePath} has been removed.")
    }

    def allFilesOutOfDate(String stdOut) {
        stdOut.contains(STD_OUT_ALL_OUT_OF_DATE)
    }
}

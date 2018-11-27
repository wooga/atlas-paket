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
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension

class PaketUnityChangeSpec extends IntegrationSpec {

    final static String STD_OUT_ALL_OUT_OF_DATE = "All input files are considered out-of-date for incremental task"

    String project1Name = "Project.1"
    String project2Name = "Project.2"
    String project3Name = "Project.3"

    File project1ReferencesFile
    File project2ReferencesFile
    File project3ReferencesFile

    List<String> project1References
    List<String> project2References
    List<String> project3References

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()

        and: "define default project references"
        project1References = ["D1", "D2", "D3"]
        project2References = ["D2", "D4"]
        project3References = ["D3", "D4"]

        and: "create multiple projects"
        project1ReferencesFile = createOrUpdateReferenceFile(project1Name, project1References)
        project2ReferencesFile = createOrUpdateReferenceFile(project2Name, project2References)
        project3ReferencesFile = createOrUpdateReferenceFile(project3Name, project3References)
    }

    @Unroll
    def "task :paketUnityInstall when #message was up to date #wasUpToDate"() {
        given: "a root project with a unity project  called #unityProjectName"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(rootDependencies)

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(unityProjectName, projectReferences)

        and: "paketUnityInstall is executed"
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        and:
        createOrUpdateReferenceFile(unityProjectName, projectReferenceUpdate)

        when: "paketUnityInstall is executed again"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME) == wasUpToDate

        fileExists("${unityProjectName}/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")

        appliedReferencesAfterUpdate.every { ref ->
            fileExists("${unityProjectName}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        where:
        unityProjectName | rootDependencies   | projectReferences | projectReferenceUpdate | wasUpToDate | message
        "Project"        | ["D1"]             | ["D1"]            | []                     | false       | "change remove all"
        "Project"        | ["D1"]             | ["D1"]            | ["D1"]                 | true        | "mime time change"
        "Project"        | ["D1", "D2"]       | ["D1"]            | ["D2"]                 | false       | "change remove one dependency"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | ["D2"]                 | false       | "change remove one dependency"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | ["D1", "D2"]           | true        | "mime time change"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | []                     | false       | "change remove all multiple"
        "Project"        | ["D1", "D2", "D3"] | ["D3"]            | ["D1", "D2", "D4"]     | false       | "remove and change and ignore not available"

        appliedReferences = rootDependencies.intersect(projectReferences)
        appliedReferencesAfterUpdate = rootDependencies.intersect(projectReferenceUpdate)
    }

    def "run paketUnityInstall a root project with multiple unity projects"() {
        given: "a dependencies file"
        def dependencies = ["D1", "D2", "D4"]
        createDependencies(dependencies)

        when: "paketUnityInstall is executed"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)

        project1References.intersect(dependencies).each { ref ->
            assert fileExists("${project1Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        project2References.intersect(dependencies).each { ref ->
            assert fileExists("${project2Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        project3References.intersect(dependencies).each { ref ->
            assert fileExists("${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }
    }

    def "task :paketInstall is incremental"() {
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(["D1", "D2"])
        def dep1 = createFile("packages/D1/content/ContentFile.cs")
        def dep2 = createFile("packages/D2/content/ContentFile.cs")

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile("Test", ["D1", "D2"])

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
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(project3References)
        def dep1 = createFile("packages/${project3References[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${project3References[1]}/content/ContentFile.cs")

        def out1 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[0]}/ContentFile.cs")
        def out2 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(project1Name, project3References)

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
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(project3References)
        def dep1 = createFile("packages/${project3References[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${project3References[1]}/content/ContentFile.cs")

        def out1 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[0]}/ContentFile.cs")
        def out2 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(project1Name, project3References)

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
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(project3References)
        def dep1 = createFile("packages/${project3References[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${project3References[1]}/content/ContentFile.cs")

        def out1 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[0]}/ContentFile.cs")
        def out2 = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${project3References[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(project1Name, project3References)

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
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(project3References)
        createFile("packages/${project3References[0]}/content/ContentFile.cs")
        createFile("packages/${project3References[1]}/content/ContentFile.cs")

        def paketDir = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}")

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(project1Name, project3References)

        and: "a file matching the file pattern"
        def baseDir = (location == "root") ? paketDir : new File(paketDir, "some/nested/directory")
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
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(project3References)
        def dep1 = createFile("packages/${project3References[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${project3References[1]}/content/ContentFile.cs")

        def paketDir = new File(projectDir, "${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}")

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(project1Name, project3References)

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

    private File createOrUpdateReferenceFile(String projectName, List<String> references) {
        def path = "${projectName}/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}"
        def referencesFile = createFile(path)
        referencesFile.text = """${references.join("\r")}""".stripIndent()
        referencesFile
    }

    private File createDependencies(List<String> dependencies) {
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """source https://nuget.org/api/v2
nuget ${dependencies.join("\nnuget ")}""".stripIndent()

        dependencies.each { dependency ->
            createFile("packages/${dependency}/content/ContentFile.cs")
        }
        createLockFile(dependencies)
        dependenciesFile
    }

    private File createLockFile(List<String> dependencies) {
        def lockFile = createFile("paket.lock")
        lockFile << """
NUGET
    remote: https://wooga.artifactoryonline.com/wooga/api/nuget/atlas-nuget
        ${dependencies.join("\n")}""".stripIndent()
        lockFile
    }

}

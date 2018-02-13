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

package wooga.gradle.paket.unity

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.get.PaketGetPlugin

class PaketUnityIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Shared
    def bootstrapTestCases = [PaketUnityPlugin.INSTALL_TASK_NAME]

    @Unroll
    def "skips paket call with [only-if] when no [paket.dependencies] file is present when running #taskToRun"(String taskToRun) {
        given: "an empty paket dependency and lock file"
        createFile("paket.lock")
        createFile("paket.unity3d.references")

        when:
        def result = runTasks(taskToRun)

        then:
        result.wasSkipped(taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "never be [UP-TO-DATE] for task #taskToRun"(String taskToRun) {
        given: "empty paket dependency file and lock"
        createFile("paket.dependencies")
        createFile("paket.lock")

        when: "running task 2x times"
        runTasksSuccessfully(taskToRun)
        def result = runTasksSuccessfully(taskToRun)

        then: "should never be [UP-TO-DATE]"
        !result.wasUpToDate(taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "run paketUnityInstall after #taskToRun"(String taskToRun) {
        given: "a small test nuget package"
        def nuget = "Mini"

        and: "apply paket get plugin to get paket unity install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file and lock"
        def dependencies = createFile("paket.dependencies")
        createFile("paket.lock")

        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget
        """.stripIndent()

        and: "the future packages directory"
        def packagesDir = new File(projectDir, 'packages')
        assert !packagesDir.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)

        where:
        taskToRun                        | _
        PaketGetPlugin.INSTALL_TASK_NAME | _
        PaketGetPlugin.UPDATE_TASK_NAME  | _
        PaketGetPlugin.RESTORE_TASK_NAME | _
        "paketUpdateMini"                | _
    }

    def "run paketUnityInstall with dependencies"() {
        given: "a small project with a unity project dir"
        def unityProjectName = "Test.Project"
        def dependencyName = "Wooga.TestDependency"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "apply paket unity plugin to get paket unity install task"
        buildFile << """
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()

        and: "paket dependency file and lock"
        def dependencies = createFile("paket.dependencies")

        dependencies << """
        source https://nuget.org/api/v2
        ${dependencyName}
          """.stripIndent()

        and: "paket unity references file "
        def references = createFile("${unityProjectName}/paket.unity3d.references")
        references << """
        ${dependencyName}
        """.stripIndent()

        and: "dependency package with file"
        createFile("packages/${dependencyName}/content/${dependencyName}.cs")

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)
        def packagesDir = new File(projectDir, "${unityProjectName}/Assets/Paket.Unity3D/${dependencyName}")
        assert packagesDir.exists()

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
    }

    @Unroll
    def "run paketUnityInstall for project #unityProjectName with dependencies #rootDependencies and references #projectReferences #shouldBeExecuted"() {
        given: "a root project with a unity project  called #unityProjectName"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "apply paket unity plugin to get paket unity install task"
        buildFile << """
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(rootDependencies.toArray() as String[])

        and: "unity project #unityProjectName with references #projectReferences"
        createUnityProject(unityProjectName, projectReferences.toArray() as String[])

        when: "paketUnityInstall is executed"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        appliedReferences.each { ref ->
            assert new File("${unityProjectName}/Assets/${ref}")
        }

        where:
        unityProjectName | rootDependencies | projectReferences | shouldBeExecuted
        "Project"        | ["D1", "D2"]     | ["D1"]            | true
        "Project"        | ["D1"]           | ["D1", "D2"]      | true
        "Project"        | ["D1", "D2"]     | ["D1", "D2"]      | true

        appliedReferences = rootDependencies.intersect(projectReferences)

    }

    private void createUnityProject(String projectName, String[] references) {
        def referencesFile = createFile("${projectName}/paket.unity3d.references")
        referencesFile << """${references.join("\r")}""".stripIndent()
    }

    private void createDependencies(String[] dependencies) {
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """source https://nuget.org/api/v2
        ${dependencies.join("\r")}""".stripIndent()

        dependencies.each { dependency ->
            createFile("packages/${dependency}/content/ContentFile.cs")
        }
    }

    boolean hasNoSource(ExecutionResult result, String taskName) {
        containsOutput(result.standardOutput, taskName, "NO-SOURCE")
    }

    private boolean containsOutput(String stdout, String taskName, String stateIdentifier) {
        stdout.contains("$taskName $stateIdentifier".toString())
    }
}

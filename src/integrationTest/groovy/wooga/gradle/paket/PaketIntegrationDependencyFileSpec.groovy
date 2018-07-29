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

package wooga.gradle.paket

import spock.lang.Unroll
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension
import wooga.gradle.paket.unity.PaketUnityPlugin

abstract class PaketIntegrationDependencyFileSpec extends PaketIntegrationBaseSpec {

    @Unroll
    def "skips paket call with [NO-SOURCE] when no [paket.dependencies] file is present when running #taskToRun"(String taskToRun) {
        given: "no dependency file"
        def futureDependenciesFile = new File(projectDir, 'paket.dependencies')
        assert !futureDependenciesFile.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        hasNoSource(result, bootstrapTaskName)
        hasNoSource(result,taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll("wpotlwr #taskToRun")
    def "writes paket output to logfile when running #taskToRun"(String taskToRun) {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        and: "the log file"
        def logFile = new File(projectDir, "build/logs/${taskToRun}.log")
        assert !logFile.exists()

        when:
        runTasksSuccessfully(taskToRun)

        then:
        logFile.exists()

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll("verify logout path #taskToRun")
    def "can set alternate logout path in task configuration when running #taskToRun"(String taskToRun) {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        and: "the log file"
        def logFile = new File(projectDir, "build/mylogs/${taskToRun}.log")
        assert !logFile.exists()

        and: "the changed log location"
        buildFile << """
            project.afterEvaluate {
                project.tasks.getByName("${taskToRun}") {
                    logFile = "build/mylogs/${taskToRun}.log"
                }
            }
        """.stripIndent()

        when:
        runTasksSuccessfully(taskToRun)

        then:
        logFile.exists()

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll("verify logout path as method #taskToRun")
    def "can set alternate logout path in task configuration as method when running #taskToRun"(String taskToRun) {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        and: "the log file"
        def logFile = new File(projectDir, "build/mylogs/${taskToRun}.log")
        assert !logFile.exists()

        and: "the changed log location"
        buildFile << """
            project.afterEvaluate {
                project.tasks.getByName("${taskToRun}") {
                    logFile("build/mylogs/${taskToRun}.log")
                }
            }
        """.stripIndent()

        when:
        runTasksSuccessfully(taskToRun)

        then:
        logFile.exists()

        where:
        taskToRun << bootstrapTestCases
    }

    //run paketUnityInstall on a real project with dependencies
    def "rpIoarpwd"() {
        given: "a dependencies file"
        fork=true
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """
            source https://nuget.org/api/v2
            nuget Mini
            nuget Wooga.Lambda
        """.stripIndent().trim()

        and: "a project with a paket.unity3d.references file"
        def referencesFile = createFile("Test/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")
        referencesFile << """
        Mini
        Wooga.Lambda
        """.stripIndent()

        and: "apply paket plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()

        when: "paketInstall is executed"
        def result = runTasksSuccessfully("paketInstall")

        then: "evaluate incremental task execution"
        result.wasExecuted("paketInstall")
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
    }
}

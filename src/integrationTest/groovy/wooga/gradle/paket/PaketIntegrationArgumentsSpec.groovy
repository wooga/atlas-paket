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

package wooga.gradle.paket

import spock.lang.IgnoreIf
import spock.lang.Unroll

abstract class PaketIntegrationArgumentsSpec extends IntegrationSpec {

    abstract Class getTestPlugin()
    abstract List<String> getTestTasks()

    def setup() {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        buildFile << """
            group = 'test'
            ${applyPlugin(getTestPlugin())}
        """.stripIndent()
    }

    @IgnoreIf({ env["CI"] })
    @Unroll
    def "task: #taskToRun is available"() {
        when:
        def result = runTasksSuccessfully("tasks")

        then:
        result.standardOutput.contains(taskToRun)

        where:
        taskToRun << getTestTasks()
    }

    @Unroll
    def "task :#taskToRun can set custom arguments '-customFlag1, -customFlag2' with args(arguments,...)"() {
        given:
        buildFile << """

        project.tasks."${taskToRun}" {
            args($value)
        }
        """.stripIndent()

        when:
        def result = runTasks(taskToRun)

        then:
        result.wasExecuted(taskToRun)
        result.standardOutput.contains("Starting process 'command '")
        result.standardOutput.contains(" $expectedCommandlineSwitch")

        where:
        taskToRun << getTestTasks()
        value = '"-customFlag1", "-customFlag2"'
        expectedCommandlineSwitch = '-customFlag1 -customFlag2'
    }

    @Unroll
    def "task :#taskToRun can set custom arguments '-customFlag1, -customFlag2' with args([arguments])"() {
        given:
        buildFile << """
        
        project.tasks."${taskToRun}" {
            args($value) 
        }
        """.stripIndent()

        when:
        def result = runTasks(taskToRun)

        then:
        result.wasExecuted(taskToRun)
        result.standardOutput.contains("Starting process 'command '")
        result.standardOutput.contains(" $expectedCommandlineSwitch")

        where:
        taskToRun << getTestTasks()
        value = '["-customFlag1", "-customFlag2"]'
        expectedCommandlineSwitch = '-customFlag1 -customFlag2'
    }

    @Unroll
    def "task :#taskToRun can set custom arguments '-customFlag1, -customFlag2' with args = [arguments]"() {
        given:
        buildFile << """
        
        project.tasks."${taskToRun}" {
            args = $value 
        }
        """.stripIndent()

        when:
        def result = runTasks(taskToRun)

        then:
        result.wasExecuted(taskToRun)
        result.standardOutput.contains("Starting process 'command '")
        result.standardOutput.contains(" $expectedCommandlineSwitch")

        where:
        taskToRun << getTestTasks()
        value = '["-customFlag1", "-customFlag2"]'
        expectedCommandlineSwitch = '-customFlag1 -customFlag2'
    }
}

package wooga.gradle.paket

import nebula.test.IntegrationSpec
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

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

package wooga.gradle.paket.get

import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationDependencyFileSpec
import wooga.gradle.paket.unity.PaketUnityPlugin

class PaketInstallIntegrationSpec extends PaketIntegrationDependencyFileSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    @Override
    Object getBootstrapTestCases() {
        return [
                PaketGetPlugin.INSTALL_TASK_NAME,
                PaketGetPlugin.UPDATE_TASK_NAME,
                PaketGetPlugin.RESTORE_TASK_NAME,
                PaketGetPlugin.OUTDATED_TASK_NAME
        ]
    }

    @Unroll
    def "installs dependencies when running #taskToRun"(String taskToRun) {
        given: "A small test nuget package"
        def nuget = "Mini"

        and: "a paket dependency file"
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """
            source https://nuget.org/api/v2
            
            nuget $nuget
        """.stripIndent()

        and: "the future packages directory"
        def packagesDir = new File(projectDir, 'packages')
        assert !packagesDir.exists()

        when:
        def result = runTasksSuccessfully('paketInstall')

        then: "paket runs and creates the packages directory"
        packagesDir.exists()

        and: "downloads the nuget package"
        result.standardOutput =~ /(?ms)Resolving packages for group Main:.*- $nuget/

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

    def "generated dependencies update task never show up in tasks list"() {
        given: "A small test nuget package"
        def nuget = "Mini"

        and: "paket dependency file and lock"
        def dependencies = createFile("paket.dependencies")
        createFile("paket.lock")

        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("tasks")

        then:
        !result.standardOutput.contains("paketUpdateMini")
    }

    def "generated dependencies update task shows up in tasks --all list"() {
        given: "A small test nuget package"
        def nuget = "Mini"

        and: "paket dependency file and lock"
        def dependencies = createFile("paket.dependencies")
        createFile("paket.lock")

        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("tasks", "--all")

        then:
        result.standardOutput.contains("paketUpdateMini")
        result.standardOutput.contains("Update $nuget to their latest version and update projects.")
    }

    //generated dependencies update task updates only one dependency
    def "gdutuood"() {
        given: "A few small test nugets"
        def nuget1 = "Mini"
        def nuget2 = "String.Extensions"

        and: "some version constrains"
        def version1 = "0.4"
        def version2 = "1.0"

        def initConstraint1 = "= $version1"
        def updateConstraint1 = "~> $version1"

        def initConstraint2 = "= $version2"
        def updateConstraint2 = "~> $version2"

        and: "paket dependency file"
        def dependencies = createFile("paket.dependencies")

        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget1 $initConstraint1
        nuget $nuget2 $initConstraint2
        """.stripIndent()

        and: "the future packages directory"
        def packagesDir = new File(projectDir, 'packages')
        assert !packagesDir.exists()

        and: "the future lock file"
        def lockFile = new File(projectDir, 'paket.lock')
        assert !lockFile.exists()

        when: "run paket install"
        runTasksSuccessfully("paketInstall")

        then:
        packagesDir.exists()
        lockFile.exists()
        lockFile.text.contains("$nuget1 ($version1)")
        lockFile.text.contains("$nuget2 ($version2)")

        when: "changing dependencies and run partial update"

        dependencies.text = ""
        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget1 $updateConstraint1
        nuget $nuget2 $updateConstraint2
        """.stripIndent()

        def result = runTasksSuccessfully("paketUpdate$nuget1")

        then:
        !lockFile.text.contains("$nuget1 ($version1)")
        lockFile.text.contains("$nuget2 ($version2)")
    }
}

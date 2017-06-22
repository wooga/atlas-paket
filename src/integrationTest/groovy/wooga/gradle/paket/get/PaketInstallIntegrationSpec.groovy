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

import spock.lang.Ignore
import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationDependencyFileSpec

class PaketInstallIntegrationSpec extends PaketIntegrationDependencyFileSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    @Override
    Object getBootstrapTestCases() {
        return [PaketGetPlugin.INSTALL_TASK_NAME, PaketGetPlugin.UPDATE_TASK_NAME, PaketGetPlugin.RESTORE_TASK_NAME]
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

    @Ignore
    @Unroll
    def "Increment task #taskToRun"(String taskToRun) {

    }
}

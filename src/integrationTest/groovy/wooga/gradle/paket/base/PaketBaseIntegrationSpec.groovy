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

package wooga.gradle.paket.base

import org.gradle.language.base.plugins.LifecycleBasePlugin
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationBaseSpec
import wooga.gradle.paket.get.PaketGetPlugin

class PaketBaseIntegrationSpec extends PaketIntegrationBaseSpec {

    def setupSpec() {
        System.setProperty("USE_DEPENDENCY_TEST", "NO")
    }

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    @Override
    Object getBootstrapTestCases() {
        return [PaketBasePlugin.INIT_TASK_NAME]
    }

    @Shared
    private List<String> paketDirectories = ["packages", ".paket", "paket-files"]

    @Unroll
    def "task :#taskToRun deletes paket directories"() {
        given: "a directory with paket directories"
        def paketDirectories = paketDirectories.collect { new File(projectDir, it) }
        paketDirectories.each { it.mkdirs() }
        assert paketDirectories.every { it.exists() && it.isDirectory() }

        when:
        runTasksSuccessfully(taskToRun)

        then:
        paketDirectories.every { !it.exists() }

        where:
        taskToRun = LifecycleBasePlugin.CLEAN_TASK_NAME
    }

    def "paketBootstrap is [UP-TO-DATE] when paket version is still the same"() {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        and: "a first run of #taskToRun"
        cleanupPaketDirectory()
        runTasksSuccessfully(taskToRun)

        when: "running a second time without changes"
        def result = runTasksSuccessfully(taskToRun)

        then: "bootstrap task was [UP-TO-DATE]"
        result.wasUpToDate(bootstrapTaskName)

        when: "delete bootstrapper"
        def paketDir = new File(projectDir, '.paket')
        def paketBootstrap = new File(paketDir, bootstrapperFileName)
        paketBootstrap.delete()

        and: "run the task again"
        def result2 = runTasksSuccessfully(taskToRun)

        then:
        result2.wasUpToDate(bootstrapTaskName)

        where:
        taskToRun = "paketBootstrap"
    }

}

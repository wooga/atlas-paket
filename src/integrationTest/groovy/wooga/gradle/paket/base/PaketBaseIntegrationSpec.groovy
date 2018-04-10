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

}

/*
 * Copyright 2017 the original author or authors.
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

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class PaketInstallIntegrationSpec extends IntegrationSpec{

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    def "skips install call with [NO-SOURCE] when no [paket.dependencies] file is present"() {
        when:
        def result = runTasksSuccessfully('paketInstall')

        then:
        hasNoSource(result, "paketBootstrap")
        hasNoSource(result,"paketInstall")
    }

    def "calls paketBootstrap"() {
        given:
        createFile("paket.dependencies")

        when:
        def result = runTasksSuccessfully('paketInstall')

        then:
        result.wasExecuted("paketBootstrap")
    }


    //

    boolean hasNoSource(ExecutionResult result, String taskName) {
        containsOutput(result.standardOutput, taskName, "NO-SOURCE")
    }

    private boolean containsOutput(String stdout, String taskName, String stateIdentifier) {
        stdout.contains("$taskName $stateIdentifier".toString())
    }
}

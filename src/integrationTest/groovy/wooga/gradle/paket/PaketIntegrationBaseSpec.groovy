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

package wooga.gradle.paket

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.Shared
import spock.lang.Unroll

abstract class PaketIntegrationBaseSpec extends IntegrationSpec{

    @Shared
    def bootstrapTestCases

    @Unroll
    def "calls paketBootstrap when running #taskToRun"(String taskToRun) {
        given: "an empty paket dependency file\""
        createFile("paket.dependencies")

        and: "future paket directories with files"
        def paketDir = new File(projectDir, '.paket')
        def paketBootstrap = new File(paketDir, 'paket.bootstrapper.exe')
        def paket = new File(paketDir, 'paket.exe')

        assert !paketDir.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasExecuted("paketBootstrap")
        paketDir.exists()
        paketBootstrap.exists()
        paket.exists()

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "skips paket call with [NO-SOURCE] when no [paket.dependencies] file is present when running #taskToRun"(String taskToRun) {
        given: "no dependency file"
        def dependenciesFile = new File(projectDir, 'paket.dependencies')
        assert !dependenciesFile.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        hasNoSource(result, "paketBootstrap")
        hasNoSource(result,taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    boolean hasNoSource(ExecutionResult result, String taskName) {
        containsOutput(result.standardOutput, taskName, "NO-SOURCE")
    }

    private boolean containsOutput(String stdout, String taskName, String stateIdentifier) {
        stdout.contains("$taskName $stateIdentifier".toString())
    }
}

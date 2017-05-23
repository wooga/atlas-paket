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

    @Shared
    String bootstrapTaskName = "paketBootstrap"

    @Shared
    String bootstrapperFileName = "paket.bootstrapper.exe"

    @Unroll
    def "calls paketBootstrap when running #taskToRun"(String taskToRun) {
        given: "an empty paket dependency and lock file"
        createFile("paket.dependencies")
        createFile("paket.lock")

        and: "future paket directories with files"
        def paketDir = new File(projectDir, '.paket')
        def paketBootstrap = new File(paketDir, bootstrapperFileName)
        def paket = new File(paketDir, 'paket.exe')

        assert !paketDir.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasExecuted(bootstrapTaskName)
        paketDir.exists()
        paketBootstrap.exists()
        paket.exists()

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "skip bootstrap when files are [UP-TO-DATE] when running #taskToRun"(String taskToRun) {
        given: "a paket dependency file"
        createFile("paket.dependencies")

        and: "a empty lock file"
        createFile("paket.lock")

        and: "a first run of #taskToRun"
        runTasksSuccessfully(taskToRun)

        when: "running a second time without changes"
        def result = runTasksSuccessfully(taskToRun)

        then: "bootstrap task was [UP-TO-DATE]"
        result.wasUpToDate(bootstrapTaskName)

        when:"delete bootstrapper"
        def paketDir = new File(projectDir, '.paket')
        def paketBootstrap = new File(paketDir, bootstrapperFileName)
        paketBootstrap.delete()

        and:"run the task again"
        def result2 = runTasksSuccessfully(taskToRun)

        then:
        !result2.wasUpToDate(bootstrapTaskName)

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
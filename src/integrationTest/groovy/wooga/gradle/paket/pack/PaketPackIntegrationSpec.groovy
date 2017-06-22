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

package wooga.gradle.paket.pack

import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationDependencyFileSpec

class PaketPackIntegrationSpec extends PaketIntegrationDependencyFileSpec {

    def paketTemplateFile
    def version = "1.0.0"
    def packageID = "Wooga.Test"

    def setup() {
        buildFile << """
            group = 'test'
            version = "$version"
            ${applyPlugin(PaketPackPlugin)}
        """.stripIndent()

        paketTemplateFile = createFile("paket.template")
        paketTemplateFile << """
            type file
            id $packageID
            authors Wooga
            owners Wooga
            description
                Empty nuget package.
        """.stripIndent()

        createFile("paket.lock")
    }

    @Override
    Object getBootstrapTestCases() {
        ["paketPack-WoogaTest"]
    }

    @Unroll
    def "writes nuget packages to output directory when running #taskToRun"(String taskToRun) {
        given:
        def outputFile = new File(new File(new File(projectDir, 'build'), "outputs"), "${packageID}.${version}.nupkg")
        assert !outputFile.exists()

        and: "a empty paket.dependencies file"
        createFile("paket.dependencies")

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        outputFile.exists()
        result.wasExecuted("paketPack-WoogaTest")

        where:
        taskToRun << ["paketPack-WoogaTest", "buildNupkg", "assemble"]
    }
}

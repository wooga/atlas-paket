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

package wooga.gradle.paket.unity

import nebula.test.IntegrationSpec
import spock.lang.Unroll
import wooga.gradle.extensions.PaketDependency
import wooga.gradle.extensions.PaketDependencySetup
import wooga.gradle.extensions.PaketUnity
import wooga.gradle.extensions.PaketUnitySetup
import wooga.gradle.paket.get.PaketGetPlugin

class PaketUnityAssemblyDefinitionStrategySpec extends IntegrationSpec {

    @PaketDependency
    PaketDependencySetup _paketSetup

    @PaketUnity
    PaketUnitySetup unityProject

    def setup() {
        buildFile << """
        ${applyPlugin(PaketGetPlugin)}
        ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Unroll
    def "task :paketInstall #message in #location paket install directory when assembly definition strategy is set to #strategy"() {
        given:
        buildFile << """
        paketUnity.assemblyDefinitionFileStrategy = "$strategy"
        """.stripIndent()

        and: "a file matching the file pattern"
        def baseDir = (location == "root") ? unityProject.installDirectory : new File(unityProject.installDirectory, "some/nested/directory")
        baseDir.mkdirs()

        def fileToKeep = createFile("test${filePattern}", baseDir) << "random content"

        when:
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        fileToKeep.exists() == expectFileToExist

        where:
        filePattern    | location | strategy   | expectFileToExist
        ".asmdef"      | "root"   | "manual"   | true
        ".asmdef"      | "nested" | "manual"   | true
        ".asmdef.meta" | "root"   | "manual"   | true
        ".asmdef.meta" | "nested" | "manual"   | true
        ".asmdef"      | "root"   | "disabled" | false
        ".asmdef"      | "nested" | "disabled" | false
        ".asmdef.meta" | "root"   | "disabled" | false
        ".asmdef.meta" | "nested" | "disabled" | false
        message = (expectFileToExist) ? "keeps files with $filePattern" : "cleans assembly definition files"
    }
}

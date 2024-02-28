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

import groovy.json.JsonSlurper
import nebula.test.IntegrationSpec
import spock.lang.Unroll
import wooga.gradle.paket.FrameworkRestriction
import wooga.gradle.paket.PaketIntegrationSpec
import wooga.gradle.paket.PaketPlugin
import wooga.gradle.paket.base.utils.internal.PaketDependencies
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension

class PaketUnityFrameworksSpec extends PaketIntegrationSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketPlugin)}
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Unroll("install #includeFrameworks")
    def "paket install"() {
        given: "a dependency file with json.net"
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """
            source https://nuget.org/api/v2
            nuget ${dependency} = ${dependencyVersion}
        """.stripIndent()

        dependenciesFile << "\n${frameworksString}"

        and: "a reference file"
        def referencesFile = createFile("${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")
        referencesFile << """${dependency}""".stripIndent()

        when: "running task:PaketInstall"
        runTasksSuccessfully("paketInstall")

        then:
        includeFrameworks.every {
            def out1 = new File(projectDir, "Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${dependency}/${it}")
            out1.exists()
        }

        !excludedFrameworks.every {
            def out2 = new File(projectDir, "Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${dependency}/${it}")
            out2.exists()
        }

        where:
        includeFrameworks           | excludedFrameworks | dependency        | dependencyVersion
        ["net20"]                   | ["net35", "net46"] | "Newtonsoft.Json" | "11.0.1"
        ["net20", "net35", "net45"] | ["net46"]          | "Newtonsoft.Json" | "11.0.1"
        ["netstandard2.0"]          | ["net20"]          | "NSubstitute"     | "5.1.0"

        frameworksString = includeFrameworks ? "framework: ${includeFrameworks.join(",")}" : ""
    }

    @Unroll("task:paketUnityInstall dlls")
    def "task:paketUnityInstall installs dlls without framework specification"() {
        given: "a dependency file with json.net"
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """
            source https://nuget.org/api/v2
            nuget ${dependency} = ${dependencyVersion}
        """.stripIndent()

        and: "a reference file"
        def referencesFile = createFile("${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")
        referencesFile << """${dependency}""".stripIndent()

        when: "running task:PaketInstall"
        runTasksSuccessfully("paketInstall")

        then:
        def out1 = new File(projectDir, "Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${dependency}/${expectedDLL}")
        out1.exists()

        where:
        dependency     | dependencyVersion | expectedDLL
        "BouncyCastle" | "1.8.4"           | "BouncyCastle.Crypto.dll"
    }

    // Yes, I had to do a shorthand because of Windows longpath exceptions.
    @Unroll
    def "cpd"() {

        given: "a generated unity project"
        def unityProjDir = generateUnityProject("unity", rooted)

        and: "generated paket files"
        def paketDeps = new PaketDependencies()
            .withNugetSource()
            .withDependency(nugetId, version)
            .withFrameworks(FrameworkRestriction.NetStandard2)

        generateDependenciesFile(paketDeps)
        generateReferencesFile(paketDeps, unityProjDir)

        and: "a configured plugin"
        appendToTask("paketUnity", "paketUpmPackageEnabled = true")
        appendToTask("paketUnity", "includeAssemblyDefinitions = true")
        if (namespace != null) {
            appendToTask("paketUnity", "defaultNamespace = ${wrapValueBasedOnType(namespace, String)}")
        }

        when:
        def result = runTasks("paketInstall")

        then:
        result.success
        def packageDirectory = new File(unityProjDir, "Packages/${upmId}")
        def packageManifestFile = new File(packageDirectory, "package.json")

        packageManifestFile.file

        def packageManifest = new JsonSlurper().parse(packageManifestFile) as Map<String, Object>
        packageManifest['name'] == upmId

        where:
        rooted | nugetId       | version | namespace                      | upmId
        false  | "NSubstitute" | "5.1.0" | null                           | "com.wooga.nuget.nsubstitute"
        false  | "NSubstitute" | "5.1.0" | "com.wooga.nuget.netstandard2" | "com.wooga.nuget.netstandard2.nsubstitute"
        true  | "NSubstitute" | "5.1.0" | null                           | "com.wooga.nuget.nsubstitute"
        true  | "NSubstitute" | "5.1.0" | "com.wooga.nuget.netstandard2" | "com.wooga.nuget.netstandard2.nsubstitute"
        id = "${nugetId[0]}${version[0]}"
    }

}

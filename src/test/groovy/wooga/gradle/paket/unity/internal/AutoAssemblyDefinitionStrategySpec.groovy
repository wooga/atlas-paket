/*
 * Copyright 2019 Wooga GmbH
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

package wooga.gradle.paket.unity.internal

import groovy.json.JsonSlurper
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AutoAssemblyDefinitionStrategySpec extends Specification {

    @Rule
    TestName testName = new TestName()

    @Shared
    File paketInstallDir

    @Subject
    @Shared
    AutoAssemblyDefinitionStrategy strategy = new AutoAssemblyDefinitionStrategy()

    def setup() {
        def base = new File("build/test/${this.class.simpleName}")
        def testOutput = new File(base, testName.methodName.replaceAll(/\W+/, '-')).absoluteFile
        if (testOutput.exists()) {
            testOutput.deleteDir()
        }

        testOutput.mkdirs()
        paketInstallDir = new File(testOutput, DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY)
        paketInstallDir.mkdirs()
    }

    def "generates base definition file"() {
        expect:
        strategy.execute(paketInstallDir)
        strategy.assemblyDefinitionPathForDirectory(paketInstallDir).exists()
    }

    @Unroll
    def "generates definition files nested Editor directories"() {
        given:
        File editorDir = new File(paketInstallDir, editorDirPath)
        editorDir.mkdirs()

        expect:
        strategy.execute(paketInstallDir)
        strategy.assemblyDefinitionPathForDirectory(paketInstallDir).exists()
        strategy.assemblyDefinitionPathForDirectory(editorDir).exists()

        where:
        editorDirPath             | _
        "Editor"                  | _
        "Nested/Directory/Editor" | _
        "Nested/Editor"           | _
    }

    def "sets name of base definition file to install directory name"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir)
        def definition = jsonSlurper.parse(strategy.assemblyDefinitionPathForDirectory(paketInstallDir))

        then:
        definition["name"] == "${paketInstallDir.name}"
    }

    @Unroll
    def "sets name of Editor definition at '#editorDirPath' to '#expectedDefinitionName'"() {
        given:
        File editorDir = new File(paketInstallDir, editorDirPath)

        editorDir.mkdirs()

        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir)
        def definition = jsonSlurper.parse(strategy.assemblyDefinitionPathForDirectory(editorDir))

        then:
        definition["name"] == expectedDefinitionName

        where:
        editorDirPath             | expectedDefinitionName
        "Editor"                  | "${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}Editor"
        "Nested/Directory/Editor" | "${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}NestedDirectoryEditor"
        "Nested/Editor"           | "${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}NestedEditor"
    }

    def "base definition has no references"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir)
        def definition = jsonSlurper.parse(strategy.assemblyDefinitionPathForDirectory(paketInstallDir))

        then:
        definition["references"] == []
    }

    @Unroll
    def "editor definitions have reference to base"() {
        given:
        File editorDir = new File(paketInstallDir, editorDirPath)
        editorDir.mkdirs()

        when:
        strategy.execute(paketInstallDir)
        def definition = readDefinition(editorDir)

        then:
        definition["references"] == [DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY]

        where:
        editorDirPath             | _
        "Editor"                  | _
        "Nested/Directory/Editor" | _
        "Nested/Editor"           | _
    }

    def "base definition sets no platforms"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir)
        def definition = jsonSlurper.parse(strategy.assemblyDefinitionPathForDirectory(paketInstallDir))

        then:
        definition["includePlatforms"] == []
    }

    @Unroll
    def "editor definitions include Editor platform"() {
        given:
        File editorDir = new File(paketInstallDir, editorDirPath)

        editorDir.mkdirs()

        when:
        strategy.execute(paketInstallDir)
        def definition = readDefinition(editorDir)

        then:
        definition["includePlatforms"] == ["Editor"]

        where:
        editorDirPath             | _
        "Editor"                  | _
        "Nested/Directory/Editor" | _
        "Nested/Editor"           | _
    }

    def readDefinition(File definition) {
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parse(strategy.assemblyDefinitionPathForDirectory(definition))
    }
}

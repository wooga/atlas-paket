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
        strategy.execute(paketInstallDir, [:])
        strategy.assemblyDefinitionPathForDirectory(paketInstallDir).exists()
    }

    @Unroll
    def "generates definition files nested Editor directories"() {
        given:
        File editorDir = new File(paketInstallDir, editorDirPath)
        editorDir.mkdirs()

        expect:
        strategy.execute(paketInstallDir, ["Dependency.1": [].toSet()])
        strategy.assemblyDefinitionPathForDirectory(paketInstallDir).exists()
        definitionFileForDirectory(editorDir).exists()

        where:
        editorDirPath                          | _
        "Dependency.1/Editor"                  | _
        "Dependency.1/Nested/Directory/Editor" | _
        "Dependency.1/Nested/Editor"           | _
    }

    def "sets name of base definition file to install directory name"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir, [:])
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
        strategy.execute(paketInstallDir, ["Dependency.1": [].toSet()])
        def definition = readDefinition(editorDir)

        then:
        definition["name"] == expectedDefinitionName

        where:
        editorDirPath                          | expectedDefinitionName
        "Dependency.1/Editor"                  | "Dependency.1.Editor"
        "Dependency.1/Nested/Directory/Editor" | "Dependency.1.Nested.Directory.Editor"
        "Dependency.1/Nested/Editor"           | "Dependency.1.Nested.Editor"
    }

    def "base definition has no references"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir, [:])
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
        strategy.execute(paketInstallDir, ["Dependency.1": [].toSet()])
        def definition = readDefinition(editorDir)

        then:
        definition["references"] == ["Dependency.1"]

        where:
        editorDirPath                          | _
        "Dependency.1/Editor"                  | _
        "Dependency.1/Nested/Directory/Editor" | _
        "Dependency.1/Nested/Editor"           | _
    }

    def "base definition sets no platforms"() {
        when:
        def jsonSlurper = new JsonSlurper()
        strategy.execute(paketInstallDir, [:])
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
        strategy.execute(paketInstallDir, ["Dependency.1": [].toSet()])
        def definition = readDefinition(editorDir)

        then:
        definition["includePlatforms"] == ["Editor"]

        where:
        editorDirPath                          | _
        "Dependency.1/Editor"                  | _
        "Dependency.1/Nested/Directory/Editor" | _
        "Dependency.1/Nested/Editor"           | _
    }

    def "creates cross references based on dependency tree"() {
        given: "A paket install"
        File depenency1 = new File(paketInstallDir, "Dependency.1")
        File depenency2 = new File(paketInstallDir, "Dependency.2")
        File depenency3 = new File(paketInstallDir, "Dependency.3")

        [depenency1, depenency2, depenency3].each { it.mkdirs() }

        when:
        strategy.execute(paketInstallDir, [
                "Dependency.1": [].toSet(),
                "Dependency.2": ["Dependency.1"].toSet(),
                "Dependency.3": ["Dependency.1", "Dependency.2"].toSet()
        ])

        then:
        def definition1 = readDefinition(depenency1)
        def definition2 = readDefinition(depenency2)
        def definition3 = readDefinition(depenency3)

        definition1["references"] == []
        definition2["references"] == ["Dependency.1"]
        definition3["references"] == ["Dependency.1", "Dependency.2"]
    }

    def "creates cross references based on dependency tree with Editor assemblies"() {
        given: "A paket install"
        File depenency1 = new File(paketInstallDir, "Dependency.1")
        File depenency2 = new File(paketInstallDir, "Dependency.2")
        File depenency3 = new File(paketInstallDir, "Dependency.3")
        File depenency4 = new File(paketInstallDir, "Dependency.4")
        File depenency1Editor = new File(depenency1, "Editor")
        File depenency2Editor = new File(depenency2, "Editor")
        File depenency4Editor = new File(depenency4, "Editor")

        [depenency1Editor, depenency2Editor, depenency3, depenency4Editor].each { it.mkdirs() }

        when:
        strategy.execute(paketInstallDir, [
                "Dependency.1": [].toSet(),
                "Dependency.2": ["Dependency.1"].toSet(),
                "Dependency.3": ["Dependency.1", "Dependency.2"].toSet(),
                "Dependency.4": ["Dependency.1", "Dependency.3"].toSet()
        ])

        then:
        def definition1 = readDefinition(depenency1Editor)
        def definition2 = readDefinition(depenency2Editor)
        def definition3 = readDefinition(depenency3)
        def definition4 = readDefinition(depenency4Editor)

        definition1["references"] == ["Dependency.1"]
        definition2["references"] == ["Dependency.2", "Dependency.1", "Dependency.1.Editor"]
        definition3["references"] == ["Dependency.1", "Dependency.2"]
        definition4["references"] == ["Dependency.4", "Dependency.1", "Dependency.3", "Dependency.1.Editor"]
    }

    def "uses existing asmdef files when available"() {
        given: "A paket install"
        File depenency1 = new File(paketInstallDir, "Dependency.1")
        File depenency2 = new File(paketInstallDir, "Dependency.2")
        File depenency3 = new File(paketInstallDir, "Dependency.3")
        File depenency4 = new File(paketInstallDir, "Dependency.4")
        File depenency1Editor = new File(depenency1, "Editor")
        File depenency2Editor = new File(depenency2, "Editor")
        File dependency2AsmDef = new File(depenency2, "SomeName.asmdef")
        File dependency2EditorAsmDef = new File(depenency2Editor, "SomeName.Editor.asmdef")
        File depenency4Editor = new File(depenency4, "Editor")

        [depenency1Editor, depenency2Editor, depenency3, depenency4Editor].each { it.mkdirs() }

        new AssemblyDefinition(dependency2AsmDef, ["Dependency.1"]).export()
        new AssemblyDefinition(dependency2EditorAsmDef, ["Dependency.1", generateDefinitionNameForDirectory(depenency1Editor)]).export()

        when:
        strategy.execute(paketInstallDir, [
                "Dependency.1": [].toSet(),
                "Dependency.2": ["Dependency.1"].toSet(),
                "Dependency.3": ["Dependency.1", "Dependency.2"].toSet(),
                "Dependency.4": ["Dependency.1", "Dependency.3"].toSet()
        ])

        then:
        def definition1 = readDefinition(depenency1Editor)
        def definition2 = readDefinition(depenency2Editor)
        def definition3 = readDefinition(depenency3)
        def definition4 = readDefinition(depenency4Editor)

        definition1["references"] == ["Dependency.1"]
        definition2["references"] == ["Dependency.2", "Dependency.1", "Dependency.1.Editor"]
        definition3["references"] == ["Dependency.1", "Dependency.2"]
        definition4["references"] == ["Dependency.4", "Dependency.1", "Dependency.3", "Dependency.1.Editor"]
    }


    String generateDefinitionNameForDirectory(File directory) {
        def dirUri = directory.toURI()
        def installDirUri = paketInstallDir.toURI()
        def relativeDirUri = installDirUri.relativize(dirUri)

        relativeDirUri.toString().split('/').collect({ it.capitalize() }).join(".")
    }

    File definitionFileForDirectory(File directory) {
        new File(directory, "${generateDefinitionNameForDirectory(directory)}.asmdef")
    }

    def readDefinition(File directory) {
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parse(definitionFileForDirectory(directory))
    }
}

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
import wooga.gradle.extensions.PaketDependencyInterceptor
import wooga.gradle.extensions.PaketDependencySetup
import wooga.gradle.extensions.PaketUnity
import wooga.gradle.extensions.PaketUnitySetup
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.tasks.PaketUnwrapUPMPackages

class PaketUnityChangeSpec extends IntegrationSpec {

    final static String STD_OUT_ALL_OUT_OF_DATE = "The input changes require a full rebuild for incremental task"
    final static String U = PaketDependencyInterceptor.localUPMWrapperPackagePrefix;

    @PaketDependency(projectDependencies = ["D1", "D2", "D3"])
    PaketDependencySetup paketSetup

    @PaketUnity(projectReferences = ["D1", "D2", "D3"])
    PaketUnitySetup unityProject1

    @PaketUnity(projectReferences = ["D2", "D4"])
    PaketUnitySetup unityProject2

    @PaketUnity(projectReferences = ["D3", "D4"])
    PaketUnitySetup unityProject3


    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Unroll
    def "task :paketUnityInstall when #message was up to date #wasUpToDate"() {
        given: "a root project with a unity project"

        and: "paket dependency file"
        paketSetup.createDependencies(rootDependencies)

        and: "unity project #unityProjectName with references #projectReferences"
        unityProject1.createOrUpdateReferenceFile(projectReferences)

        and: "paketUnityInstall is executed"
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        and:
        unityProject1.createOrUpdateReferenceFile(projectReferenceUpdate)

        when: "paketUnityInstall is executed again"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME) == wasUpToDate

        unityProject1.projectReferencesFile.exists()

        appliedReferencesAfterUpdate.every { ref ->
            new File(unityProject1.installDirectory, ref as String).exists()
        }

        where:
        rootDependencies   | projectReferences | projectReferenceUpdate | wasUpToDate | message
        ["D1"]             | ["D1"]            | []                     | false       | "change remove all"
        ["D1"]             | ["D1"]            | ["D1"]                 | true        | "mime time change"
        ["D1", "D2"]       | ["D1"]            | ["D2"]                 | false       | "change remove one dependency"
        ["D1", "D2"]       | ["D1", "D2"]      | ["D2"]                 | false       | "change remove one dependency"
        ["D1", "D2"]       | ["D1", "D2"]      | ["D1", "D2"]           | true        | "mime time change"
        ["D1", "D2"]       | ["D1", "D2"]      | []                     | false       | "change remove all multiple"
        ["D1", "D2", "D3"] | ["D3"]            | ["D1", "D2", "D4"]     | false       | "remove and change and ignore not available"

        appliedReferences = rootDependencies.intersect(projectReferences)
        appliedReferencesAfterUpdate = rootDependencies.intersect(projectReferenceUpdate)
    }

    def "run paketUnityInstall a root project with multiple unity projects"() {
        when: "paketUnityInstall is executed"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)

        unityProject1.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject1.installDirectory, ref as String).exists()
        }

        unityProject2.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject2.installDirectory, ref as String).exists()
        }

        unityProject3.projectReferences.intersect(paketSetup.projectDependencies).each { ref ->
            assert new File(unityProject3.installDirectory, ref as String).exists()
        }
    }

    @Unroll
    def "task :paketUnityUnwrapUPMPackages when #message was up to date #wasUpToDate"() {
        given: "a root project with a unity project"

        and: "paket dependency file"
        paketSetup.createDependencies(rootDependencies)

        and: "unity project #unityProjectName with references #projectReferences"
        unityProject1.createOrUpdateReferenceFile(projectReferences)

        and: "paketUnityUnwrapUPMPackages is executed"
        runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        and:
        unityProject1.createOrUpdateReferenceFile(projectReferenceUpdate)

        when: "paketUnityInstall is executed again"
        def result = runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        result.wasUpToDate(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME) == wasUpToDate

        unityProject1.projectReferencesFile.exists()

        appliedReferencesAfterUpdate.every { ref ->
            if (ref.startsWith(PaketDependencyInterceptor.localUPMWrapperPackagePrefix)) {
                new File(unityProject1.getUPMInstallDirectory(), (ref as String) + "/dummy_file").exists()
            } else {
                !(new File(unityProject1.getUPMInstallDirectory(), (ref as String) + "/dummy_file").exists())
            }
        }

        where:
        rootDependencies               | projectReferences     | projectReferenceUpdate          | wasUpToDate | message
        ["${U}D1"]                     | ["${U}D1"]            | []                              | false       | "change remove all"
        ["${U}D1"]                     | ["${U}D1"]            | ["${U}D1"]                      | true        | "mime time change"
        ["${U}D1", "${U}D2"]           | ["${U}D1"]            | ["${U}D2"]                      | false       | "change remove one dependency"
        ["${U}D1", "${U}D2"]           | ["${U}D1", "${U}D2"]  | ["${U}D2"]                      | false       | "change remove one dependency"
        ["${U}D1", "${U}D2"]           | ["${U}D1", "${U}D2"]  | ["${U}D1", "${U}D2"]            | true        | "mime time change"
        ["${U}D1", "${U}D2"]           | ["${U}D1", "${U}D2"]  | []                              | false       | "change remove all multiple"
        ["${U}D1", "${U}D2", "${U}D3"] | ["${U}D3"]            | ["${U}D1", "${U}D2", "${U}D4"]  | false       | "remove and change and ignore not available"
        ["${U}D1"]                     | [""]                  | ["${U}D1"]                      | false       | "change add a dependency"
        ["${U}D1", "D2"]               | ["${U}D1"]            | ["${U}D1", "D2"]                | false       | "change add non-wrapped dependency"

        appliedReferences = rootDependencies.intersect(projectReferences)
        appliedReferencesAfterUpdate = rootDependencies.intersect(projectReferenceUpdate)
    }

    def "task :paketUnityUnwrapUPMPackages updates UPM Wrapped packages versions"() {
        given: "a root project with a unity project"
        unityProject1.createOrUpdateReferenceFile(["${U}D1"])

        and: "paket dependency file with an UPM wrapped Package"
        paketSetup.createDependencies(["${U}D1"])

        and: "a upm wrapped package with version 0.0.1"
        def f = createFile("packages/${U}D1/lib/paket.upm.wrapper.reference")
        f.text = "${U}D1.tgz;${U}D1@0.0.1"

        when: "install version 0.0.1"
        runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        def unwrapped_1 = new File(unityProject1.getUPMInstallDirectory(), "${U}D1@0.0.1/dummy_file")

        assert unwrapped_1.exists()

        and: "update to version 0.0.2"
        f.text = "${U}D1.tgz;${U}D1@0.0.2"
        runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        then:
        def unwrapped_2 = new File(unityProject1.getUPMInstallDirectory(), "${U}D1@0.0.2/dummy_file")

        !unwrapped_1.exists()
        unwrapped_2.exists()
    }

    def "task :paketInstall is incremental"() {
        given: "a root project with a unity project"
        and: "paket dependency file"
        paketSetup.createDependencies(["D1", "D2"])
        def dep1 = createFile("packages/D1/content/ContentFile.cs")
        def dep2 = createFile("packages/D2/content/ContentFile.cs")

        and: "unity project #unityProjectName with references #projectReferences"
        unityProject1.createOrUpdateReferenceFile(["D1", "D2"])

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        when: "change source file in dependencies"
        dep1 << "change"

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        containsHasChangedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)
    }

    def "task :paketInstall removes incrementally with changed source"() {
        given: "a root project with a unity project"
        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        dep1.delete()

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        containsHasRemovedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        !out1.exists()
        out2.exists()
    }

    def "task :paketUnityUnwrapUPMPackages ignores change in not managed UPM package"() {
        given: "a root project with a unity project"
        unityProject1.createOrUpdateReferenceFile(["${U}D1"])

        and: "paket dependency file with an UPM wrapped Package and an unmanaged foo package"
        paketSetup.createDependencies(["${U}D1"])
        def out1 = createFile("foo/dummy_file", unityProject1.getUPMInstallDirectory())
        assert out1.exists()

        when: "install upm dependency"
        runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        def unwrapped_1 = new File(unityProject1.getUPMInstallDirectory(), "${U}D1/dummy_file")
        assert unwrapped_1.exists()

        and: "delete a file in the target directory's unmanaged upm package"
        out1.delete()
        assert !out1.exists()

        def result = runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        then: "upm unwrap task should be up-to-date"
        result.wasUpToDate(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        !containsHasDeletedOutputProperty(result.standardOutput, out1.path)
        !allFilesOutOfDate(result.standardOutput)
    }

    def "task :paketUnityUnwrapUPMPackages rebuilds unwrapped UPM with change in target directory"() {
        given: "a root project with a unity project"
        unityProject1.createOrUpdateReferenceFile(["${U}D1"])

        and: "paket dependency file with an UPM wrapped Package"
        paketSetup.createDependencies(["${U}D1"])

        when: "install upm dependency"
        runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        def unwrapped_1 = new File(unityProject1.getUPMInstallDirectory(), "${U}D1/dummy_file")
        assert unwrapped_1.exists()

        and: "delete a file in the target directory"
        unwrapped_1.delete()

        assert !unwrapped_1.exists()

        def result = runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        then:
        unwrapped_1.exists()

        result.wasExecuted(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        containsHasDeletedOutputProperty(result.standardOutput, unwrapped_1.path)

        allFilesOutOfDate(result.standardOutput)
    }


    def "task :paketInstall adds with change in target directory"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        out2.delete()

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        allFilesOutOfDate(result.standardOutput)

        out1.exists()
        out2.exists()
    }

    def "task :paketInstall adds incrementally with changed target content"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "some source and destination files"
        def dep1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def dep2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def out1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def out2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")

        assert !out1.exists()
        assert !out2.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        out1.exists()
        out2.exists()

        when: "delete source file in dependencies"
        out2 << "local patch"

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        !containsHasChangedOrDeletedOutput(result.standardOutput, dep1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, dep2.path)

        allFilesOutOfDate(result.standardOutput)

        out1.exists()
        out2.exists()
    }

    @Unroll
    def "task :paketInstall keeps files with #filePattern in #location paket install directory when strategy is #strategy"() {
        given: "a file matching the file pattern"
        def baseDir = (location == "root") ? unityProject1.installDirectory : new File(unityProject1.installDirectory, "some/nested/directory")
        baseDir.mkdirs()
        def fileToKeep = createFile("test${filePattern}", baseDir) << "random content"

        and: "the assembly file definition strategy set to manual"
        buildFile << """
        paketUnity.assemblyDefinitionFileStrategy = "$strategy"
        """.stripIndent()

        when:
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        fileToKeep.exists()

        where:
        filePattern    | location | strategy
        ".asmdef"      | "root"   | "manual"
        ".asmdef"      | "nested" | "manual"
        ".asmdef.meta" | "root"   | "manual"
        ".asmdef.meta" | "nested" | "manual"
    }

    def "task :paketInstall deletes empty directories"() {
        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")

        def paketDir = unityProject1.installDirectory

        and: "some empty directories in output directory"
        def rootDir = new File(paketDir, "dirAtRoot")
        def secondLevel = new File(rootDir, "dirAtSecondLevel")
        def thirdLevel = new File(secondLevel, "dirAtThirdLevel")
        thirdLevel.mkdirs()

        when:
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        !rootDir.exists()
        !secondLevel.exists()
        !thirdLevel.exists()
    }

    @Unroll
    def "task :paketInstall is not up to date when includeAssemblyDefinitions goes from #includeAtStart to #includeAtEnd"() {

        given: "a root project with a unity project"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "a configured plugin"
        buildFile << """
        paketUnity {
            setIncludeAssemblyDefinitions(${includeAtStart})
        }
        """.stripIndent()

        and: "some source files and an assembly definittion"
        def file1 = createFile("packages/${unityProject3.projectReferences[0]}/content/ContentFile.cs")
        def file2 = createFile("packages/${unityProject3.projectReferences[1]}/content/ContentFile.cs")
        def asmdefFile = createFile("packages/${unityProject3.projectReferences[1]}/content/Content.asmdef")

        def expectedFile1 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[0]}/ContentFile.cs")
        def expectedFile2 = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/ContentFile.cs")
        def expectedAsmdefFile = new File(unityProject3.installDirectory, "${unityProject3.projectReferences[1]}/Content.asmdef")

        assert !expectedFile1.exists()
        assert !expectedFile2.exists()
        assert !expectedAsmdefFile.exists()

        when: "running paket install at the start"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !containsHasChangedOrDeletedOutput(result.standardOutput, file1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, file2.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, asmdefFile.path)

        expectedFile1.exists()
        expectedFile2.exists()
        expectedAsmdefFile.exists() == includeAtStart

        when: "running paket install again"
        buildFile << """
        paketUnity {
            setIncludeAssemblyDefinitions(${includeAtEnd})
        }
        """.stripIndent()

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: ""
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)

        !containsHasChangedOrDeletedOutput(result.standardOutput, file1.path)
        !containsHasChangedOrDeletedOutput(result.standardOutput, file2.path)

        allFilesOutOfDate(result.standardOutput)

        expectedFile1.exists()
        expectedFile2.exists()
        expectedAsmdefFile.exists() == includeAtEnd

        where:
        includeAtStart | includeAtEnd
        true           | false
        false          | true
    }

    def containsHasChangedOrDeletedOutput(String stdOut, String filePath) {
        containsHasChangedOutput(stdOut, filePath) || containsHasRemovedOutput(stdOut, filePath)
    }

    def containsHasDeletedOutputProperty(String stdOut, String filePath) {
        stdOut.contains("Output property 'outputDirectory' file ${filePath} has been removed.")
    }

    def containsHasChangedOutput(String stdOut, String filePath) {
        stdOut.contains("inputFiles' file ${filePath} has changed.")
    }

    def containsHasRemovedOutput(String stdOut, String filePath) {
        stdOut.contains("inputFiles' file ${filePath} has been removed.")
    }

    def allFilesOutOfDate(String stdOut) {
        stdOut.contains(STD_OUT_ALL_OUT_OF_DATE)
    }
}

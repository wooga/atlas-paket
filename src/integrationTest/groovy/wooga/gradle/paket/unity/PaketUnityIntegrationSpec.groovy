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
import nebula.test.functional.ExecutionResult
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.tasks.PaketUnityInstall
import wooga.gradle.paket.unity.tasks.PaketUnwrapUPMPackages

class PaketUnityIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Shared
    def bootstrapTestCases = [PaketUnityPlugin.INSTALL_TASK_NAME, PaketUnityPlugin.UNWRAP_UPM_TASK_NAME]

    @Unroll
    def "skips paket call with [only-if] when no [paket.dependencies] file is present when running #taskToRun"(String taskToRun) {
        given: "an empty paket dependency and lock file"
        createFile("paket.lock")
        createFile("paket.unity3d.references")

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasSkipped(taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "never be [UP-TO-DATE] for task #taskToRun"(String taskToRun) {
        given: "empty paket dependency file and lock"
        createFile("paket.dependencies")
        createFile("paket.lock")

        when: "running task 2x times"
        runTasksSuccessfully(taskToRun)
        def result = runTasksSuccessfully(taskToRun)

        then: "should never be [UP-TO-DATE]"
        !result.wasUpToDate(taskToRun)

        where:
        taskToRun << bootstrapTestCases
    }

    @Unroll
    def "run paketUnityInstall and paketUnityUnwrapUPMPackages after #taskToRun"(String taskToRun) {
        given: "a small test nuget package"
        def nuget = "Mini"

        and: "apply paket get plugin to get paket unity install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file and lock"
        def dependencies = createFile("paket.dependencies")
        createFile("paket.lock")

        dependencies << """
        source https://nuget.org/api/v2
        
        nuget $nuget
        """.stripIndent()

        and: "the future packages directory"
        def packagesDir = new File(projectDir, 'packages')
        assert !packagesDir.exists()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.wasExecuted(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)

        where:
        taskToRun                        | _
        PaketGetPlugin.INSTALL_TASK_NAME | _
        PaketGetPlugin.UPDATE_TASK_NAME  | _
        PaketGetPlugin.RESTORE_TASK_NAME | _
        "paketUpdateMini"                | _
    }

    def "run paketUnityInstall with dependencies"() {
        given: "a small project with a unity project dir"
        def unityProjectName = "Test.Project"
        def dependencyName = "Wooga.TestDependency"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "setup paket configuration"
        setupPaketProject(dependencyName, unityProjectName)

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)
        def packagesDir = new File(projectDir, "${unityProjectName}/Assets/Paket.Unity3D/${dependencyName}")
        assert packagesDir.exists()

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
    }

    def "paketUnityInstall ignores UPM Wrappers"() {
        given: "a small project with a unity project dir"
        def unityProjectName = "Test.Project"
        def dependencyName = PaketUnwrapUPMPackages.localUPMWrapperPackagePrefix + "TestDependency"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "setup paket configuration"
        setupWrappedUpmPaketProject(dependencyName, unityProjectName)

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)
        def packagesDir = new File(projectDir, "${unityProjectName}/Assets/Paket.Unity3D/${dependencyName}")
        assert !packagesDir.exists()

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
    }

    def "run paketUnityUnwrapUPMPackages with dependencies"() {
        given: "a small project with a unity project dir"
        def unityProjectName = "Test.Project"
        def dependencyName = PaketUnwrapUPMPackages.localUPMWrapperPackagePrefix + "TestDependency"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "setup paket configuration with wrapped upm dep"
        setupWrappedUpmPaketProject(dependencyName, unityProjectName)

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        def packagesDir = new File(projectDir, "${unityProjectName}/Packages/${dependencyName}")
        def unpackedFile = new File(projectDir, "${unityProjectName}/Packages/${dependencyName}/dummy_file")

        then:
        packagesDir.exists()
        unpackedFile.exists()
        result.wasExecuted(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
    }

    def "paketUnityUnwrapUPMPackages ignores not wrapped packages"() {
        given: "a small project with a unity project dir"
        def unityProjectName = "Test.Project"
        def dependencyName = "TestDependency"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "setup paket configuration with wrapped upm dep"
        setupPaketProject(dependencyName, unityProjectName)

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
        def packagesDir = new File(projectDir, "${unityProjectName}/Packages/${dependencyName}")
        def unpackedFile = new File(projectDir, "${unityProjectName}/Packages/${dependencyName}/dummy_file")

        then:
        !packagesDir.exists()
        !unpackedFile.exists()
        result.wasExecuted(PaketUnityPlugin.UNWRAP_UPM_TASK_NAME)
    }

    @Unroll("Copy assembly definitions when includeAssemblyDefinitions is #includeAssemblyDefinitions and set in #configurationString")
    def "copy assembly definition files"() {

        given: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        buildFile << """
            ${configurationString} {                 
                    includeAssemblyDefinitions = ${includeAssemblyDefinitions}                  
            }      
        """.stripIndent()

        and: "setup paket configuration"
        setupPaketProject(dependencyName, unityProjectName)

        and: "setup assembly definition file in package"
        def asmdefFileName = "${dependencyName}.${PaketUnityInstall.assemblyDefinitionFileExtension}"
        def inputAsmdefFile = createFile("packages/${dependencyName}/content/${asmdefFileName}")
        assert inputAsmdefFile.exists()

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)
        def outputDir = "${unityProjectName}/Assets/Paket.Unity3D/${dependencyName}"
        def packagesDir = new File(projectDir, outputDir)
        assert packagesDir.exists()

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        def outputAsmdefFilePath = "${packagesDir}/${asmdefFileName}"
        def outputAsmdefFile = new File(outputAsmdefFilePath)
        includeAssemblyDefinitions == outputAsmdefFile.exists()

        where:
        baseConfigurationString | includeAssemblyDefinitions
        "paketUnity" | true
        "paketUnity" | false
        "project.tasks.getByName(#taskName%%)" | true
        "project.tasks.getByName(#taskName%%)" | false

        unityProjectName = "Test.Project"
        taskName = PaketUnityPlugin.INSTALL_TASK_NAME + unityProjectName
        dependencyName = "Wooga.TestDependency"
        configurationString = baseConfigurationString.replace("#taskName%%", "'${taskName}'")
    }

    private void setupPaketProject(dependencyName, unityProjectName) {

        def dependencies = createFile("paket.dependencies")
        dependencies << """
        source https://nuget.org/api/v2
        nuget ${dependencyName}
          """.stripIndent()

        def lockFile = createFile("paket.lock")
        lockFile << """${dependencyName}""".stripIndent()

        def references = createFile("${unityProjectName}/paket.unity3d.references")
        references << """
        ${dependencyName}
        """.stripIndent()

        createFile("packages/${dependencyName}/content/${dependencyName}.cs")
    }

    private void setupWrappedUpmPaketProject(dependencyName, unityProjectName) {
        setupPaketProject(dependencyName, unityProjectName)

        copyDummyTgz("packages/${dependencyName}/lib/${dependencyName}.tgz")
        def f = createFile("packages/${dependencyName}/lib/paket.upm.wrapper.reference")
        f.text = "${dependencyName}.tgz;${dependencyName}"
    }

    private File copyDummyTgz(String dest) {
        copyResources("upm_package.tgz", dest)
    }

    static boolean hasNoSource(ExecutionResult result, String taskName) {
        containsOutput(result.standardOutput, taskName, "NO-SOURCE")
    }

    static boolean containsOutput(String stdout, String taskName, String stateIdentifier) {
        stdout.contains("$taskName $stateIdentifier".toString())
    }
}

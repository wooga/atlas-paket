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

import com.wooga.gradle.test.PropertyUtils
import groovy.json.JsonSlurper
import nebula.test.functional.ExecutionResult
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.extensions.PaketDependencyInterceptor
import wooga.gradle.paket.FrameworkRestriction
import wooga.gradle.paket.PaketIntegrationSpec
import wooga.gradle.paket.base.utils.internal.PaketDependencies
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.fixtures.PaketFixturesTrait
import wooga.gradle.paket.unity.tasks.PaketUnityInstall

class PaketUnityIntegrationSpec extends PaketIntegrationSpec implements PaketFixturesTrait {

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

    @Unroll("unwrap upm after")
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
        def dependencyName = PaketDependencyInterceptor.localUPMWrapperPackagePrefix + "TestDependency"

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
        def dependencyName = PaketDependencyInterceptor.localUPMWrapperPackagePrefix + "TestDependency"

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
        baseConfigurationString                | includeAssemblyDefinitions
        "paketUnity"                           | true
        "paketUnity"                           | false
        "project.tasks.getByName(#taskName%%)" | true
        "project.tasks.getByName(#taskName%%)" | false

        unityProjectName = "Test.Project"
        taskName = PaketUnityPlugin.INSTALL_TASK_NAME + unityProjectName
        dependencyName = "Wooga.TestDependency"
        configurationString = baseConfigurationString.replace("#taskName%%", "'${taskName}'")
    }

    def "ensures Paket-installed UPM packages have the package dot json in the package root"() {
        given:
        def unityProjDir = new File(projectDir, "unity")
        new File(unityProjDir, "Assets").mkdirs()
        def (_, testPkgJson) = fakeUPMPaketPackage("test", unityProjDir)
        def pkgInstallDir = new File(unityProjDir, "Packages/com.something.test")
        def testPkgContents = testPkgJson.text

        and:
        buildFile << """
            paketUnity {
                enablePaketUpmPackages()
            }
        """

        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.success
        def pkgJsonFile = new File(pkgInstallDir, "package.json")
        pkgJsonFile.file
        pkgJsonFile.text == testPkgContents
    }

    def "ensures Paket-installed non-UPM packages #msg a generated package dot json in package root when paketUpmPackageEnabled is #paketUpmPackageEnabled"() {
        given:
        def unityProjDir = new File(projectDir, "unity")
        new File(unityProjDir, "Assets").mkdirs()
        def testNuGetPackage = fakePaketPackage("test")
        def manifestJson = new File(unityProjDir, "Packages/manifest.json").with {
            mkdirs(); createNewFile();
            it
        }
        def pktUnityInstallDir = new File(unityProjDir, "Packages/${(overrides?.get('name') ?: "com.wooga.nuget.test")}")
        assert !new File(testNuGetPackage, "package.json").exists()

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = $paketUpmPackageEnabled
                ${overrides ?
            "paketUpmPackageManifests = ['test': ${PropertyUtils.wrapValueBasedOnType(overrides, Map)}]" :
            ""
        }
            }
        """

        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.success
        def pkgJsonFile = new File(pktUnityInstallDir, "package.json")
        if (hasPackageManifest) {
            pkgJsonFile.file
            manifestJson.file
            def pkgJson = new JsonSlurper().parse(pkgJsonFile) as Map<String, Object>
            pkgJson['name'] == (overrides?.get('name') ?: "${pktUnityInstallDir.name.toLowerCase()}")
            pkgJson.entrySet().containsAll(overrides?.entrySet() ?: [:])
        } else {
            !pkgJsonFile.file
            !manifestJson.file
        }

        where:
        paketUpmPackageEnabled | hasPackageManifest | overrides                 | msg
        true                   | true               | null                      | "has"
        true                   | true               | [name: "com.custom.name"] | "has"
        true                   | true               | [custom: "customfield"]   | "has"
        false                  | false              | null                      | "hasn't"
    }

    def "Paket-installed #packageType packages install into directory named after #source upm name"() {
        given:
        def unityProjDir = new File(projectDir, "unity")
        new File(unityProjDir, "Assets").mkdirs()

        if (isUpmPackage) {
            def (_, testPkgJson) = fakeUPMPaketPackage("test", unityProjDir)
        } else {
            def testNuGetPackage = fakePaketPackage("test")
        }

        def pkgInstallDirName = isUpmPackage ? "com.something.test" : (overrides?.get('name') ?: "com.wooga.nuget.test")
        def pktUnityInstallDir = new File(unityProjDir, "Packages/${pkgInstallDirName}")

        new File(unityProjDir, "Packages").mkdirs()
        def manifestJson = new File(unityProjDir, "Packages/manifest.json")
        manifestJson.createNewFile()

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = true
                ${overrides ?
            "paketUpmPackageManifests = ['test': ${PropertyUtils.wrapValueBasedOnType(overrides, Map)}]" :
            ""
        }
            }
        """

        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.success
        def pkgJsonFile = new File(pktUnityInstallDir, "package.json")

        pkgJsonFile.file
        manifestJson.file

        def pkgJson = new JsonSlurper().parse(pkgJsonFile) as Map<String, Object>
        pkgJson['name'] == pkgInstallDirName

        pkgJson.entrySet().containsAll(overrides?.entrySet() ?: [].toSet())

        where:
        packageType   | source         | isUpmPackage | overrides
        "upm-enabled" | "package-json" | true         | null
        "non-upm"     | "generated"    | false        | null
        "non-upm"     | "overridden"   | false        | ["name": "com.custom.name"]
    }

    def "Paket-installed #packageType packages get deleted"() {
        given:
        def unityProjDir = new File(projectDir, "unity")
        new File(unityProjDir, "Assets").mkdirs()


        def pkgInstallDirName = isUpmPackage ? "com.something.test" : (overrides?.get('name') ?: "com.wooga.nuget.test")
        def pktUnityInstallDir = new File(unityProjDir, "Packages/${pkgInstallDirName}")

        File nugetPackageDir

        if (isUpmPackage) {
            def (packageFolder, testPkgJson) = fakeUPMPaketPackage("test", unityProjDir)
            nugetPackageDir = packageFolder
        } else {
            nugetPackageDir = fakePaketPackage("test")
        }

        new File(unityProjDir, "Packages").mkdirs()
        def manifestJson = new File(unityProjDir, "Packages/manifest.json")
        manifestJson.createNewFile()

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = true
                ${overrides ?
            "paketUpmPackageManifests = ['test': ${PropertyUtils.wrapValueBasedOnType(overrides, Map)}]" :
            ""
        }
            }
        """

        when:
        runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)
        // delete deps
        def dependencies = new File(projectDir, "paket.dependencies")
        def lockFile = new File(projectDir, "paket.lock")
        dependencies.text = "source https://nuget.org/api/v2"
        lockFile.text = ""
        nugetPackageDir.deleteDir()

        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.success
        !pktUnityInstallDir.exists()


        where:
        packageType          | isUpmPackage | overrides
        "upm-enabled"        | true         | null
        "non-upm"            | false        | null
        "non-upm-overridden" | false        | ["name": "com.custom.name"]
    }

    def "#deleteVerb #packageType package on non-incremental delete"() {
        given:
        def unityProjDir = new File(projectDir, "unity")
        new File(unityProjDir, "Assets").mkdirs()
        setupPaketProject("Wooga.TestDependency", "unity")

        def installedUnityPackageDir = new File(unityProjDir, "Packages/${packageId}")
        installedUnityPackageDir.mkdirs()
        def installedUnityPackageFile = new File(installedUnityPackageDir, isUpmPackage ? "package.json" : "some.file")
        installedUnityPackageFile.text = "{}"

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = true
                preInstalledUpmPackages = ["${preinstalledPackage}"]
            }
            // force non-incremental build
            tasks.named('${PaketUnityPlugin.INSTALL_TASK_NAME}').configure {
                outputs.upToDateWhen { false }
            }
        """
        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)
        then:
        result.success
        if (doesDelete) {
            !installedUnityPackageDir.exists()
            !installedUnityPackageFile.exists()
        } else {
            installedUnityPackageDir.exists()
            installedUnityPackageFile.exists()
        }

        where:
        deleteVerb        | packageType       | doesDelete | packageId                    | preinstalledPackage          | isUpmPackage
        "Does NOT delete" | "non-upm-package" | false      | "com.wooga.do-not-delete-me" | ""                           | false
        "Does NOT delete" | "preinstalled"    | false      | "com.wooga.do-not-delete-me" | "com.wooga.do-not-delete-me" | true
        "Deletes"         | "upm-package"     | true       | "com.wooga.delete-me"        | ""                           | true
    }


    def "Shared paket and unity install-dir install"() {
        given:
        def unityProjDir = projectDir
        new File(unityProjDir, "Assets").mkdirs()

        if (isUpmPackage) {
            fakeUPMPaketPackage("test", unityProjDir, new File(projectDir, "Packages"))
        } else {
            fakePaketPackage("test", projectDir, new File(projectDir, "Packages"), projectDir)
        }

        def pkgInstallDirName = isUpmPackage ? "com.something.test" : (overrides?.get('name') ?: "com.wooga.nuget.test")
        def pktUnityInstallDir = new File(unityProjDir, "Packages/${pkgInstallDirName}")

        new File(unityProjDir, "Packages").mkdirs()
        def manifestJson = new File(unityProjDir, "Packages/manifest.json")
        manifestJson.createNewFile()

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = true
                ${overrides ?
            "paketUpmPackageManifests = ['test': ${PropertyUtils.wrapValueBasedOnType(overrides, Map)}]" :
            ""
        }
            }
        """

        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.success
        def pkgJsonFile = new File(pktUnityInstallDir, "package.json")
        pkgJsonFile.file
        manifestJson.file

        def pkgJson = new JsonSlurper().parse(pkgJsonFile) as Map<String, Object>
        pkgJson['name'] == pkgInstallDirName

        pkgJson.entrySet().containsAll(overrides?.entrySet() ?: [].toSet())

        where:
        packageType   | source         | isUpmPackage | overrides
        "upm-enabled" | "package-json" | true         | null
        "non-upm"     | "generated"    | false        | null
        "non-upm"     | "overridden"   | false        | ["name": "com.custom.name"]
    }

    def "Shared paket and unity packages folder #deleteVerb #packageType package on non-incremental delete"() {
        given:
        def unityProjDir = projectDir
        new File(unityProjDir, "Assets").mkdirs()
        def dependencyFile = setupPaketProject("Wooga.TestDependency", "")

        def installedUnityPackageDir = new File(unityProjDir, "Packages/${packageId}")
        installedUnityPackageDir.mkdirs()
        def installedUnityPackageFile = new File(installedUnityPackageDir, isUpmPackage ? "package.json" : "some.file")
        installedUnityPackageFile.text = "{}"

        and:
        buildFile << """
            paketUnity {
                paketUpmPackageEnabled = true
                preInstalledUpmPackages = ["${preinstalledPackage}"]
            }
            // force non-incremental build
            tasks.named('${PaketUnityPlugin.INSTALL_TASK_NAME}').configure {
                outputs.upToDateWhen { false }
            }
        """
        when:
        def result = runTasks(PaketUnityPlugin.INSTALL_TASK_NAME)
        then:
        result.success
        if (doesDelete) {
            !installedUnityPackageDir.exists()
            !installedUnityPackageFile.exists()
        } else {
            installedUnityPackageDir.exists()
            installedUnityPackageFile.exists()
        }

        dependencyFile.exists()

        where:
        deleteVerb        | packageType       | doesDelete | packageId                    | preinstalledPackage          | isUpmPackage
        "Does NOT delete" | "non-upm-package" | false      | "com.wooga.do-not-delete-me" | ""                           | false
        "Does NOT delete" | "preinstalled"    | false      | "com.wooga.do-not-delete-me" | "com.wooga.do-not-delete-me" | true
        "Deletes"         | "upm-package"     | true       | "com.wooga.delete-me"        | ""                           | true

    }

    private File setupPaketProject(dependencyName, unityProjectName) {

        def dependencies = createFile("paket.dependencies")
        dependencies << """
        source https://nuget.org/api/v2
        nuget ${dependencyName}
          """.stripIndent()

        def lockFile = createFile("paket.lock")
        lockFile << """${dependencyName}""".stripIndent()

        def references = createFile("${(!unityProjectName?.isEmpty()) ? unityProjectName + "/" : ""}paket.unity3d.references")
        references << """
        ${dependencyName}
        """.stripIndent()

        def packagesName = unityProjectName?.isEmpty() ? "Packages" : "packages";

        return createFile("${packagesName}/${dependencyName}/content/${dependencyName}.cs")
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

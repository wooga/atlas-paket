package wooga.gradle.paket.unity

import nebula.test.IntegrationSpec
import spock.lang.Unroll
import wooga.gradle.paket.get.PaketGetPlugin

class PaketUnityChangeSpec extends IntegrationSpec {

    String project1Name = "Project.1"
    String project2Name = "Project.2"
    String project3Name = "Project.3"

    File project1ReferencesFile
    File project2ReferencesFile
    File project3ReferencesFile

    List<String> project1References
    List<String> project2References
    List<String> project3References

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()

        and: "define default project references"
        project1References = ["D1", "D2", "D3"]
        project2References = ["D2", "D4"]
        project3References = ["D3", "D4"]

        and: "create multiple projects"
        project1ReferencesFile = createOrUpdateReferenceFile(project1Name, project1References)
        project2ReferencesFile = createOrUpdateReferenceFile(project2Name, project2References)
        project3ReferencesFile = createOrUpdateReferenceFile(project3Name, project3References)
    }

    @Unroll
    def "task :paketUnityInstall when #message was up to date #wasUpToDate"() {
        given: "a root project with a unity project  called #unityProjectName"

        and: "apply paket get plugin to get paket install task"
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(rootDependencies)

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile(unityProjectName, projectReferences)

        and: "paketUnityInstall is executed"
        runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        and:
        createOrUpdateReferenceFile(unityProjectName, projectReferenceUpdate)

        when: "paketUnityInstall is executed again"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME) == wasUpToDate

        fileExists("${unityProjectName}/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")

        appliedReferencesAfterUpdate.every { ref ->
            fileExists("${unityProjectName}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        where:
        unityProjectName | rootDependencies   | projectReferences | projectReferenceUpdate | wasUpToDate | message
        "Project"        | ["D1"]             | ["D1"]            | []                     | false       | "change remove all"
        "Project"        | ["D1"]             | ["D1"]            | ["D1"]                 | true        | "mime time change"
        "Project"        | ["D1", "D2"]       | ["D1"]            | ["D2"]                 | false       | "change remove one dependency"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | ["D2"]                 | false       | "change remove one dependency"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | ["D1", "D2"]           | true        | "mime time change"
        "Project"        | ["D1", "D2"]       | ["D1", "D2"]      | []                     | false       | "change remove all multiple"
        "Project"        | ["D1", "D2", "D3"] | ["D3"]            | ["D1", "D2", "D4"]     | false       | "remove and change and ignore not available"

        appliedReferences = rootDependencies.intersect(projectReferences)
        appliedReferencesAfterUpdate = rootDependencies.intersect(projectReferenceUpdate)
    }

    def "run paketUnityInstall a root project with multiple unity projects"() {
        given: "a dependencies file"
        def dependencies = ["D1", "D2", "D4"]
        createDependencies(dependencies)

        when: "paketUnityInstall is executed"
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then: "evaluate incremental task execution"
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)

        project1References.intersect(dependencies).each { ref ->
            assert fileExists("${project1Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        project2References.intersect(dependencies).each { ref ->
            assert fileExists("${project2Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }

        project3References.intersect(dependencies).each { ref ->
            assert fileExists("${project3Name}/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${ref}")
        }
    }

    def "task :paketInstall is incremental"() {
        given:
        buildFile << """
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()

        and: "paket dependency file"
        createDependencies(["D1","D2"])
        def dep1 = createFile("packages/D1/content/ContentFile.cs")
        def dep2 = createFile("packages/D2/content/ContentFile.cs")

        and: "unity project #unityProjectName with references #projectReferences"
        createOrUpdateReferenceFile("Test", ["D1","D2"])

        when:
        def result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.standardOutput.contains("inputFiles' file ${dep1.path} has changed.")
        !result.standardOutput.contains("inputFiles' file ${dep2.path} has changed.")

        when: "change source file in dependencies"
        dep1 << "change"

        result = runTasksSuccessfully(PaketUnityPlugin.INSTALL_TASK_NAME)

        then:
        result.wasExecuted(PaketUnityPlugin.INSTALL_TASK_NAME)
        !result.wasUpToDate(PaketUnityPlugin.INSTALL_TASK_NAME)
        result.standardOutput.contains("inputFiles' file ${dep1.path} has changed.")
        !result.standardOutput.contains("inputFiles' file ${dep2.path} has changed.")
    }


    private File createOrUpdateReferenceFile(String projectName, List<String> references) {
        def path = "${projectName}/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}"
        def referencesFile = createFile(path)
        referencesFile.text = """${references.join("\r")}""".stripIndent()
        referencesFile
    }

    private File createDependencies(List<String> dependencies) {
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """source https://nuget.org/api/v2
        ${dependencies.join("\r")}""".stripIndent()

        dependencies.each { dependency ->
            createFile("packages/${dependency}/content/ContentFile.cs")
        }
        dependenciesFile
    }

}

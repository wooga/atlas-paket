package wooga.gradle.paket.unity

import nebula.test.IntegrationSpec
import spock.lang.Unroll
import wooga.gradle.paket.PaketPlugin

class PaketUnityFrameworksSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketPlugin)}
            ${applyPlugin(PaketUnityPlugin)}
        """.stripIndent()
    }

    @Unroll("task:paketUnityInstall wf")
    def "task:paketUnityInstall with frameworks:#includeFrameworks"() {
        given: "a project with a unity project"
        def dependency = "Newtonsoft.Json"

        and: "a dependency file with json.net"
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile << """source https://nuget.org/api/v2
        nuget ${dependency} = 11.0.1""".stripIndent()
        dependenciesFile << "\n${frameworksString}"

        and: "a reference file"
        def referencesFile = createFile("${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}")
        referencesFile << """${dependency}""".stripIndent()

        when: "running task:PaketInstall"
        runTasksSuccessfully("paketInstall")

        then:
        includeFrameworks.each {
            def out1 = new File(projectDir, "Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${dependency}/${it}")
            out1.exists()
        }
        excludedFrameworks.each {
            def out2 = new File(projectDir, "Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}/${dependency}/${it}")
            !out2.exists()
        }

        where:
        includeFrameworks           | excludedFrameworks
        ["net20"]                   | ["net35", "net46"]
        ["net20", "net35", "net45"] | ["net46"]
        frameworksString = includeFrameworks ? "framework: ${includeFrameworks.join(",")}" : ""
    }

}
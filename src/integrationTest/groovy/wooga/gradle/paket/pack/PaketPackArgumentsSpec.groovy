package wooga.gradle.paket.pack

import wooga.gradle.paket.PaketIntegrationArgumentsSpec

class PaketPackArgumentsSpec extends PaketIntegrationArgumentsSpec {

    @Override
    Class getTestPlugin() {
        return PaketPackPlugin.class
    }

    @Override
    List<String> getTestTasks() {
        return ["paketPack-WoogaTest"]
    }

    def paketTemplateFile
    def version = "1.0.0"
    def packageID = "Wooga.Test"

    def setup() {
        buildFile << """
            version = "$version"
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
}

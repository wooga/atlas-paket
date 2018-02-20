package wooga.gradle.paket.base.utils.internal

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PaketDependenciesSpec extends Specification {

    static String DEPENDENCIES_CONTENT = """
    source https://nuget.org/api/v2
    source https://nuget.org/api/v3
    lala: skdjskdjsd        
    nuget Dependency.One = 0.1.0
    nuget Dependency.Two ~> 2.0.1

    // Files from GitHub repositories.
    github forki/FsUnit FsUnit.fs

    // Gist files.
    gist Thorium/1972349 timestamp.fs

    // HTTP resources.
    http http://www.fssnip.net/1n decrypt.fs
                         
    framework: net35, net40

    group Test
        source https://nuget.org/api/v2
        source https://nuget.org/api/v3
        source https://nuget.org/api/v4
        
        nuget Dependency.One = 0.1.0
        nuget Dependency.Two ~> 2.0.1
        nuget Dependency.Three ~> 2.0.1
        // Files from GitHub repositories.
        github forki/FsUnit FsUnit.fs

        // Gist files.
        gist Thorium/1972349 timestamp.fs

        // HTTP resources.
        http http://www.fssnip.net/1n decrypt.fs
    """.stripIndent()

    @Shared
    File dependenciesFile = File.createTempFile("paket", ".dependencies")

    @Unroll
    def "initialize with #objectType"() {
        expect:
        new PaketDependencies(content)

        where:
        objectType | content
        "String"   | DEPENDENCIES_CONTENT
        "File"     | dependenciesFile << DEPENDENCIES_CONTENT
    }

    @Unroll
    def "parses nuget dependencies from paket.dependencies with #objectType"() {
        when:
        def dependencies = new PaketDependencies(content)

        then:
        def nugets = dependencies.nugetDependencies
        nugets.size() == 3
        nugets.contains("Dependency.One")
        nugets.contains("Dependency.Two")
        nugets.contains("Dependency.Three")

        where:
        objectType | content
        "String"   | DEPENDENCIES_CONTENT
        "File"     | dependenciesFile << DEPENDENCIES_CONTENT
    }

    @Unroll
    def "parses sources from paket.dependencies with #objectType"() {
        when:
        def dependencies = new PaketDependencies(content)

        then:
        def sources = dependencies.sources
        sources.size() == 3
        sources.contains("https://nuget.org/api/v2")
        sources.contains("https://nuget.org/api/v3")
        sources.contains("https://nuget.org/api/v4")

        where:
        objectType | content
        "String"   | DEPENDENCIES_CONTENT
        "File"     | dependenciesFile << DEPENDENCIES_CONTENT
    }

    @Unroll
    def "returns an empty list for #property when no #data data is available in dependencies file"() {
        when:
        def dependencies = new PaketDependencies("")

        then:
        List<String> list = dependencies.invokeMethod(property, null) as List<String>
        list.isEmpty()

        where:
        property               | data
        "getSources"           | "source"
        "getNugetDependencies" | "nuget"
    }

    @Unroll
    def "parses frameworks from paket.dependencies with #objectType"() {
        when:
        def dependencies = new PaketDependencies(content)

        then:
        def frameworks = dependencies.frameworks
        frameworks.size() == 2
        frameworks.contains("net35")
        frameworks.contains("net40")

        where:
        objectType | content
        "String"   | DEPENDENCIES_CONTENT
        "File"     | dependenciesFile << DEPENDENCIES_CONTENT
    }

    @Unroll
    def "Set default framework if not defined"() {
        when:
        def content = """
        source https://nuget.org/api/v2
        source https://nuget.org/api/v3
        nuget Dependency.One = 0.1.0
        nuget Dependency.Two ~> 2.0.1
        """.stripIndent()

        def dependencies = new PaketDependencies(content)

        then:
        def frameworks = dependencies.frameworks
        frameworks.contains("net35")
    }
}

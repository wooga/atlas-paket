package wooga.gradle.paket.base.utils.internal

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PaketLockSpec extends Specification {

    static String LOCK_CONTENT = """
    NUGET
      remote: https://wooga.artifactoryonline.com/wooga/api/nuget/atlas-nuget
        NSubstitute (1.9.1.0)
        Substance (0.0.10-beta)
        Substance.Collections (0.0.10-beta)
          Substance (>= 0.0.10-beta)
        Substance.Collections.Generic (0.0.10-beta)
          Substance (>= 0.0.10-beta)
          Substance.Collections (>= 0.0.10-beta)
        Substance.Collections.Immutable (0.0.10-beta)
          Substance (>= 0.0.10-beta)
          Substance.Collections (>= 0.0.10-beta)
          Substance.Collections.Generic (>= 0.0.10-beta)
        Wooga.AtlasBuildTools (1.0.0)
        Wooga.Lambda (0.7.0)
          Substance.Collections.Immutable (>= 0.0.0-beta)
        Wooga.XCodeEditor (3.1.3)
          Wooga.JsonDotNetNode (>= 0.1.0-prerelease < 0.2.0-prerelease)
      remote: https://wooga.artifactoryonline.com/wooga/api/nuget/atlas-nuget-snapshot
        Wooga.JsonDotNetNode (0.1.1-master00001)
    """.stripIndent()

    static String LOCK_CONTENT_2 = """
    NUGET
      remote: https://wooga.artifactoryonline.com//wooga/api/nuget/atlas-nuget
        Substance (0.0.10-beta)
        Wooga.AssetBundleManager (3.0.0-rc00001)
          Wooga.Services (>= 3.0.0-rc)
        Wooga.EditorSpotlight (1.0.0)
        Wooga.NativeShare (0.1.0)
        Wooga.Services (3.0.1)
          Wooga.JsonDotNetNode (>= 0.1.0-prerelease < 0.2.0-prerelease)
          Wooga.Lambda (>= 0.7 < 1.0)
      remote: https://www.nuget.org/api/v2
        Substance.Collections (0.0.10-beta)
          Substance (>= 0.0.10-beta)
        Substance.Collections.Generic (0.0.10-beta)
          Substance (>= 0.0.10-beta)
          Substance.Collections (>= 0.0.10-beta)
        Substance.Collections.Immutable (0.0.10-beta)
          Substance (>= 0.0.10-beta)
          Substance.Collections (>= 0.0.10-beta)
          Substance.Collections.Generic (>= 0.0.10-beta)
        Wooga.Lambda (0.7)
          Substance.Collections.Immutable (>= 0.0.0-beta)
      remote: https://wooga.artifactoryonline.com//wooga/api/nuget/atlas-nuget-snapshot
        Wooga.JsonDotNetNode (0.1.1-master00001)
    """.stripIndent()

    @Shared
    File lockFile = File.createTempFile("paket", ".lock")

    @Unroll
    def "initialize with #objectType"() {
        expect:
        new PaketDependencies(content)

        where:
        objectType | content
        "String"   | LOCK_CONTENT
        "File"     | lockFile << LOCK_CONTENT
    }

    @Unroll
    def "parses nuget dependencies from paket.lock with #objectType"() {
        when:
        def lock = new PaketLock(content)

        then:
        def nugets = lock.getDependencies(PaketLock.SourceType.NUGET, "Wooga.XCodeEditor")
        nugets.size() == 1
        nugets.contains("Wooga.JsonDotNetNode")

        where:
        objectType | content
        "String"   | LOCK_CONTENT
        "File"     | lockFile << LOCK_CONTENT
    }

    @Unroll
    def "parses all nuget dependencies from paket.lock with #objectType"() {
        when:
        def lock = new PaketLock(content)

        then:
        def nugets = lock.getAllDependencies(references)

        expectedDependencies.every {
            nugets.contains(it)
        }

        nugets.size() == expectedDependencies.size()

        where:
        objectType | content
        "String"   | LOCK_CONTENT_2
        "File"     | lockFile << LOCK_CONTENT_2

        references = ["Wooga.Services", "Wooga.AssetBundleManager", "Wooga.NativeShare", "Wooga.EditorSpotlight"]
        expectedDependencies = references + ["Substance","Substance.Collections","Substance.Collections.Immutable", "Substance.Collections.Generic", "Wooga.Lambda", "Wooga.JsonDotNetNode"]
    }

}

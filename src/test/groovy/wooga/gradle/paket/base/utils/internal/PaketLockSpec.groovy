package wooga.gradle.paket.base.utils.internal

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PaketLockSpec extends Specification {

    static String LOCK_CONTENT = """
    NUGET
      remote: https://a.repo.com
        A (0.0.10-beta)
        W.A (3.0.0-rc00001)
          W.S (>= 3.0.0-rc)
        W.E (1.0.0)
        W.N (0.1.0)
        W.S (3.0.1)
          W.J (>= 0.1.0-prerelease < 0.2.0-prerelease)
          W.L (>= 0.7 < 1.0)
      remote: https://www.nuget.org/api/v2
        A.C (0.0.10-beta)
          A (>= 0.0.10-beta)
        A.C.G (0.0.10-beta)
          A (>= 0.0.10-beta)
          A.C (>= 0.0.10-beta)
        A.C.I (0.0.10-beta)
          A (>= 0.0.10-beta)
          A.C (>= 0.0.10-beta)
          A.C.G (>= 0.0.10-beta)
        W.L (0.7)
          A.C.I (>= 0.0.0-beta)
      remote: https://a.repo.com/snapshot
        W.J (0.1.1-master00001)
    """.stripIndent()

    static String MULTI_TYPE_LOCK_CONTENT = """
    ${LOCK_CONTENT}

    GITHUB
      remote: fsharp/FAKE
        modules/Octokit/Octokit.fsx (a25c2f256a99242c1106b5a3478aae6bb68c7a93)
          Octokit (>= 0)
    GIT
      remote: https://github.com/forki/nupkgtest.git
        (05366e390e7552a569f3f328a0f3094249f3b93b)
    HTTP
      remote: http://www.fssnip.net/raw/1M/test1.fs
      test1.fs    
    """.stripIndent()

    static String LOCK_CONTENT_BROKEN_TYPE = """
    NUGIT
      remote: https://a.repo.com
        A (0.0.10-beta)
        W.A (3.0.0-rc00001)
          W.S (>= 3.0.0-rc)
        W.E (1.0.0)
        W.N (0.1.0)
        W.S (3.0.1)
          W.J (>= 0.1.0-prerelease < 0.2.0-prerelease)
          W.L (>= 0.7 < 1.0)
    """.stripIndent()

    static String LOCK_CONTENT_BROKEN_INDENTION = """
    NUGET
        remote: https://a.repo.com
            A (0.0.10-beta)
            W.A (3.0.0-rc00001)
                W.S (>= 3.0.0-rc)
            W.E (1.0.0)
            W.N (0.1.0)
            W.S (3.0.1)
                W.J (>= 0.1.0-prerelease < 0.2.0-prerelease)
                W.L (>= 0.7 < 1.0)
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
        def nugets = lock.getDependencies(PaketLock.SourceType.NUGET, "W.A")
        nugets.size() == 1
        nugets.contains("W.S")

        where:
        objectType | content
        "String"            | LOCK_CONTENT
        "File"              | lockFile << LOCK_CONTENT
        "multi type String" | MULTI_TYPE_LOCK_CONTENT
        "multi type File"   | lockFile << MULTI_TYPE_LOCK_CONTENT
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
        objectType          | content
        "String"            | LOCK_CONTENT
        "File"              | lockFile << LOCK_CONTENT
        "multi type String" | MULTI_TYPE_LOCK_CONTENT
        "multi type File"   | lockFile << MULTI_TYPE_LOCK_CONTENT

        references = ["W.S", "W.A", "W.N", "W.E"]
        expectedDependencies = references + ["A", "A.C", "A.C.I", "A.C.G", "W.L", "W.J"]
    }

    @Unroll
    def "fails to expand when dependency file has #failure"() {
        when:
        def lock = new PaketLock(content)

        then:
        def nugets = lock.getAllDependencies(references)

        expectedDependencies.every {
            nugets.contains(it)
        }

        nugets.size() == expectedDependencies.size()

        where:
        failure           | content
        "broken type"     | LOCK_CONTENT_BROKEN_TYPE
        "wrong indention" | LOCK_CONTENT_BROKEN_INDENTION
        references = ["A"]
        expectedDependencies = references
    }

}

package wooga.gradle.paket.unity

import nebula.test.ProjectSpec
import wooga.gradle.paket.unity.internal.UnityDllMetaFile

class UnityDllMetaFileSpec extends ProjectSpec {

    def "generates meta file"() {

        given: "a DLL"
        def name = "brownCow"
        def dll = new File(projectDir, "${name}.dll")
        dll.createNewFile()

        when:
        def metaFile = UnityDllMetaFile.generate(dll)

        then:
        metaFile.exists()
        metaFile.name == "${dll.name}.meta"
    }
}

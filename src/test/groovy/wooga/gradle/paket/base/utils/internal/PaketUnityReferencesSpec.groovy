package wooga.gradle.paket.base.utils.internal

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import wooga.gradle.paket.base.utils.internal.PaketTemplate

class PaketUnityReferencesSpec extends Specification {

    @Unroll
    def "ensure items are trimmed"() {

        when:
        def refs = new PaketUnityReferences(content)

        then:
        refs.nugets == nugets

        where:
        content               | nugets
        "A1\nA2\nA3\n"        | ["A1", "A2", "A3"]
        " A1 \nA2\nA3\n"      | ["A1", "A2", "A3"]
        " A1 \n A2 \nA3   \n" | ["A1", "A2", "A3"]
    }
}

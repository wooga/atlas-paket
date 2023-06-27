package wooga.gradle.paket.base.utils.internal


import spock.lang.Specification
import spock.lang.Unroll

class PaketUnityReferencesSpec extends Specification {

    @Unroll
    def "parses references"() {

        when:
        def ref = PaketUnityReference.Parse(line)
        expected.includeTests = includeTests
        expected.includeAssemblies = includeAssemblies

        then:
        ref != null
        ref.name == expected.name
        ref.includeTests == expected.includeTests
        ref.includeAssemblies == expected.includeAssemblies

        where:
        line                                                               | expected                           | includeTests | includeAssemblies
        "Foo.Bar"                                                          | new PaketUnityReference("Foo.Bar") | false        | false
        "    Foo.Bar   "                                                   | new PaketUnityReference("Foo.Bar") | false        | false
        "   Foo.Bar"                                                       | new PaketUnityReference("Foo.Bar") | false        | false
        "Foo.Bar   "                                                       | new PaketUnityReference("Foo.Bar") | false        | false
        "Foo.Bar [includeTests:true]"                                      | new PaketUnityReference("Foo.Bar") | true         | false
        "Foo.Bar   [includeAssemblies:true]"                               | new PaketUnityReference("Foo.Bar") | false        | true
        "Foo.Bar   [includeAssemblies:true,includeTests:true]"             | new PaketUnityReference("Foo.Bar") | true         | true
        "   Foo.Bar   [includeAssemblies : true,  includeTests: true]"     | new PaketUnityReference("Foo.Bar") | true         | true
        "   Foo.Bar   [  includeAssemblies : true,  includeTests: true  ]" | new PaketUnityReference("Foo.Bar") | true         | true

    }

    @Unroll
    def "ensure items are trimmed"() {

        when:
        def refs = new PaketUnityReferences(content)

        then:
        refs.referenceNames == references

        where:
        content               | references
        "A1\nA2\nA3\n"        | ["A1", "A2", "A3"]
        " A1 \nA2\nA3\n"      | ["A1", "A2", "A3"]
        " A1 \n A2 \nA3   \n" | ["A1", "A2", "A3"]
    }
}

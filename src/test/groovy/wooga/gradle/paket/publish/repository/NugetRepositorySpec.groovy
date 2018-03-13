package wooga.gradle.paket.publish.repository

import spock.lang.Specification
import wooga.gradle.paket.publish.repository.internal.NugetRepository

class NugetRepositorySpec extends Specification {

    def "applies properties"(){

        given: "a fresh NugetRepository object"
        def vo = new NugetRepository()

        when: "set properties via methods"
        vo.url("url")
        vo.name("name")
        vo.apiKey("apiKey")
        vo.endpoint("endpoint")

        then:
        vo.url == "url"
        vo.name == "name"
        vo.apiKey == "apiKey"
        vo.endpoint == "endpoint"
    }

    def "applies all properties"(){

        given: "a fresh NugetRepository object"
        def vo = new NugetRepository()

        when: "set path and url"
        vo.url("url")
        vo.path("path")

        then: "Exception gets thrown"
        thrown(Exception)
    }
}

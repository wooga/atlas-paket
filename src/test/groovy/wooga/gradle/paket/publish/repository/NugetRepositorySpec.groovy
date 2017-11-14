package wooga.gradle.paket.publish.repository

import spock.lang.Specification

class NugetRepositorySpec extends Specification {

    def "applies properties"(){

        given: "a fresh NugetRepository object"
        def vo = new NugetRepository()

        when: "set properties via methods"
        vo.url("url")
        vo.name("name")
        vo.apiKey("apiKey")

        then:
        vo.url == "url"
        vo.name == "name"
        vo.apiKey == "apiKey"
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

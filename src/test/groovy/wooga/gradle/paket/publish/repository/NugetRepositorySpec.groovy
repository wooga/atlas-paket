/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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

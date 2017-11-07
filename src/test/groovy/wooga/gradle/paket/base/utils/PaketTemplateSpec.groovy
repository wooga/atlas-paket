/*
 * Copyright 2017 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.base.utils

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PaketTemplateSpec extends Specification {

    static String PACKAGE_ID = "Test.Paket.Package"
    static String PACKAGE_VERSION = "1.0.0"

    static String TEMPLATE_CONTENT = """
    type file
    id $PACKAGE_ID
    version $PACKAGE_VERSION
    """.stripIndent()


    @Shared
    File templateFile = File.createTempFile("paket", ".template")

    @Unroll
    def "initialize with #objectType"() {
        expect:
        new PaketTemplate(content)

        where:
        objectType | content
        "String"   | TEMPLATE_CONTENT
        "File"     | templateFile << TEMPLATE_CONTENT
    }

    @Unroll
    def "parses id from template with #objectType"() {
        when:
        def template = new PaketTemplate(content)

        then:
        template.getPackageId() == PACKAGE_ID

        where:
        objectType | content
        "String"   | TEMPLATE_CONTENT
        "File"     | templateFile << TEMPLATE_CONTENT
    }

    @Unroll
    def "parses version from template with #objectType"() {
        when:
        def template = new PaketTemplate(content)

        then:
        template.getVersion() == PACKAGE_VERSION

        where:
        objectType | content
        "String"   | TEMPLATE_CONTENT
        "File"     | templateFile << TEMPLATE_CONTENT
    }

    @Unroll
    def "returns null for non existent #property"() {
        when:
        def template = new PaketTemplate("")

        then:
        template.invokeMethod(property, null) == null

        where:
        property       | _
        "getVersion"   | _
        "getPackageId" | _
    }

}

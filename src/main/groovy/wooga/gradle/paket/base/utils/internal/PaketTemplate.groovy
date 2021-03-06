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

package wooga.gradle.paket.base.utils.internal

class PaketTemplate {

    private def content

    PaketTemplate(File templateFile) {
        this(templateFile.text)
    }

    PaketTemplate(String templateContent) {
        content = [:]
        templateContent.eachLine { line ->
            def matcher
            if ((matcher = line =~ /^(\w+)( |\n[ ]{4})(((\n[ ]{4})?.*)+)/)) {
                content[matcher[0][1]] = matcher[0][3]
            }
        }
    }

    String getPackageId() {
        content['id']
    }

    String getVersion() {
        content['version']
    }
}

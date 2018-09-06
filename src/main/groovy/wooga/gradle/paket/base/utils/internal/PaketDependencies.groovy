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

class PaketDependencies implements wooga.gradle.paket.base.utils.PaketDependencies{

    final private List<String> DEFAULT_FRAMEWORKS = ["net11", "net20", "net35", "unity", "net35-unity full v3.5", "net35-unity subset v3.5"]
    // 4.6 -> ["net462", "net461", "net46", "net452", "net451", "net45", "net403", "net40", "net4"]

    PaketDependencies(File dependenciesFile) {
        this(dependenciesFile.text)
    }

    private List<String> nugets
    private List<String> dependencySources
    private List<String> frameworks

    PaketDependencies(String dependenciesContent) {
        nugets = []
        dependencySources = []

        dependenciesContent.eachLine { line ->
            def matcher
            if ((matcher = line =~ /^\s*([\w:]+)\s([\w\/:.(, )+]+)(.*)$/)) {

                def key = matcher[0][1].toString()
                def value = matcher[0][2].toString().trim()

                switch (key) {
                    case "nuget":
                        nugets << value
                        break
                    case "source":
                        dependencySources << value
                        break
                    case "framework:":
                        frameworks = value.split(",").collect { it.trim() }
                        break
                }
            }
        }
    }

    Set<String> getSources() {
        dependencySources
    }

    Set<String> getNugetDependencies() {
        nugets
    }

    List<String> getFrameworks() {
        frameworks ?: DEFAULT_FRAMEWORKS
    }
}

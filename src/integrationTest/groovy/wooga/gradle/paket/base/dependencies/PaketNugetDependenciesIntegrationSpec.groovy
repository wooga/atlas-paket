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

package wooga.gradle.paket.base.dependencies

import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationSpec
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin

class PaketNugetDependenciesIntegrationSpec extends PaketIntegrationSpec {
    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    static String NUGET_VERSION_STRING = """

    nuget "Wooga.Test ~> 3"

    """.stripIndent()

    static String NUGET_CONFIG_CLOSURE = """
    nuget("Wooga.Test") {
        version "~> 3"
    }
    """.stripIndent()

    static String NUGET_CONFIG_CLOSURE_2 = """
    nuget("Wooga.Test = 2.0.0") {
        version "~> 3"
    }
    """.stripIndent()

    static String NUGET_WITHOUT_VERSION = """

    nuget "Wooga.Test"

    """.stripIndent()

    static String ADD_NUGET_STRING = """

    add "nuget Wooga.Test ~> 3"

    """.stripIndent()

    static String ADD_NUGET_STRING_NO_VERSION = """

    add "nuget Wooga.Test"

    """.stripIndent()

    static String ADD_NUGET_STRING_NO_VERSION_CLOSURE = """

    add("nuget Wooga.Test") {
        version "~> 3"
    }

    """.stripIndent()

    static String ADD_NUGET_STRING_NO_VERSION_CLOSURE_2 = """

    add("nuget Wooga.Test = 2.0.0") {
        version "~> 3"
    }

    """.stripIndent()

    static String NUGET_MAIN_GROUP = """
    main { 
        nuget "Wooga.Test ~> 3"
    }
    """.stripIndent()

    static String NUGET_CUSTOM_GROUP = """
    custom { 
        nuget "Wooga.Test ~> 3"
    }
    """.stripIndent()

    @Unroll
    def "user can define dependencies with [#definition] in build.gradle"() {
        given: "a build file with one nuget dependency defined"
        buildFile << """
        dependencies {
            paket {
                ${definition}
            }
        }
        """.stripIndent()

        and: "a future paket.dependencies file"
        File paketDependencies = new File(projectDir, 'paket.dependencies')
        assert !paketDependencies.exists()

        when:
        runTasksSuccessfully("paketDependencies")

        then:
        paketDependencies.exists()
        def regex = '^' + expectedNugetLine + '$'
        paketDependencies.text.readLines().any { it.matches(regex) }

        where:
        definition                            | expectedNugetLine
        NUGET_VERSION_STRING                  | "nuget Wooga.Test ~> 3"
        NUGET_CONFIG_CLOSURE                  | "nuget Wooga.Test ~> 3"
        NUGET_CONFIG_CLOSURE_2                | "nuget Wooga.Test ~> 3"
        ADD_NUGET_STRING                      | "nuget Wooga.Test ~> 3"
        ADD_NUGET_STRING_NO_VERSION           | "nuget Wooga.Test"
        ADD_NUGET_STRING_NO_VERSION_CLOSURE   | "nuget Wooga.Test ~> 3"
        ADD_NUGET_STRING_NO_VERSION_CLOSURE_2 | "nuget Wooga.Test ~> 3"
        NUGET_MAIN_GROUP                      | "nuget Wooga.Test ~> 3"
        NUGET_CUSTOM_GROUP                    | "    nuget Wooga.Test ~> 3"
        NUGET_WITHOUT_VERSION                 | "nuget Wooga.Test"
    }

    static String NUGET_SOURCE_WITHOUT_CREDENTIALS = """
    nuget {
        url "https://wooga.jfrog.io/wooga/api/nuget/atlas-nuget-snapshot"        
    }
    """.stripIndent()

    static String NUGET_SOURCE_WITH_CREDENTIALS = """
    nuget {
        url "https://wooga.jfrog.io/wooga/api/nuget/atlas-nuget-snapshot"
        credentials {
            username = "foo"
            password = "bar"
        }
        
    }
    """.stripIndent()

    @Unroll
    def "user can define dependencies source #type"() {
        given: "a build file with one nuget dependency defined"
        buildFile << """
        dependencies {
            paket {
                nuget "Wooga.Test"
            }
        }
        """.stripIndent()

        and: "a custom nuget repository"
        buildFile << """
        repositories {
            ${value}   
        }
        """.stripIndent()

        and: "a future paket.dependencies file"
        File paketDependencies = new File(projectDir, 'paket.dependencies')
        assert !paketDependencies.exists()

        when:
        runTasksSuccessfully("paketDependencies")

        then:
        paketDependencies.exists()
        def regex = '^' + expectedSourceValue + '$'
        paketDependencies.text.readLines().any { it.matches(regex) }

        where:
        type                      | value                            | expectedSourceValue
        "url without credentials" | NUGET_SOURCE_WITHOUT_CREDENTIALS | "source https://wooga.jfrog.io/wooga/api/nuget/atlas-nuget-snapshot"
        "url with credentials"    | NUGET_SOURCE_WITH_CREDENTIALS    | "source https://wooga.jfrog.io/wooga/api/nuget/atlas-nuget-snapshot username: \"foo\" password: \"bar\""
    }

    static String NUGET_SOURCE_SINGLE_DIR = """
    nuget {
        dir "../path/to/source/one"
    }

    nuget {
        dir "../path/to/source/two"                
    }
    """.stripIndent()

    static String NUGET_SOURCE_MULTI_DIR = """ 
    nuget {
        dir "../path/to/source/one"
        dir "../path/to/source/two"
    }
    """.stripIndent()

    static String NUGET_SOURCE_MULTI_DIR_2 = """
    nuget {
        dirs "../path/to/source/one", "../path/to/source/two"
    }
    """.stripIndent()

    @Unroll
    def "user can define #type"() {
        given: "a build file with one nuget dependency defined"
        buildFile << """
        dependencies {
            paket {
                nuget "Wooga.Test"
            }
        }
        """.stripIndent()

        and: "a custom nuget repository"
        buildFile << """
        repositories {
            ${value}
        }
        """.stripIndent()

        and: "a future paket.dependencies file"
        File paketDependencies = new File(projectDir, 'paket.dependencies')
        assert !paketDependencies.exists()

        when:
        runTasksSuccessfully("paketDependencies")

        then:
        paketDependencies.exists()
        paketDependencies.text.contains("source ${osPath('../path/to/source/one')}")
        paketDependencies.text.contains("source ${osPath('../path/to/source/two')}")

        where:

        type                                      | value
        "single directory per repository"         | NUGET_SOURCE_SINGLE_DIR
        "multiple directories per repository"     | NUGET_SOURCE_MULTI_DIR
        "multiple directories in one instruction" | NUGET_SOURCE_MULTI_DIR_2
    }

    @Unroll
    def "user can restrict frameworks with #type"() {
        given: "a build file with one nuget dependency defined and framework restriction"
        buildFile << """
        dependencies {
            paket {
                ${value}
                nuget "Wooga.Test"
            }
        }
        """.stripIndent()

        and: "a future paket.dependencies file"
        File paketDependencies = new File(projectDir, 'paket.dependencies')
        assert !paketDependencies.exists()

        when:
        runTasksSuccessfully("paketDependencies")

        then:
        paketDependencies.exists()
        paketDependencies.text.contains(expectedFrameworkRestriction)

        where:
        type                                       | value                                                 | expectedFrameworkRestriction
        "a single framework"                       | "framework 'net35'"                                   | "framework: net35"
        "multiple frameworks with one call"        | "frameworks 'net35', 'net40'"                         | "framework: net35, net40"
        "multiple frameworks as array"             | 'frameworks(["net35", "net40", "monomac"])'           | "framework: net35, net40, monomac"
        "multiple frameworks as array with setter" | 'frameworks = ["net35", "net40"]'                     | "framework: net35, net40"
        "multiple frameworks single calls"         | "framework 'net40'; framework 'monomac'"              | "framework: net40, monomac"
        "reset frameworks with setter"             | "frameworks 'net35', 'net40'; frameworks=['monomac']" | "framework: monomac"
    }

    @Unroll
    def ":paketDependencies #method paket.dependencies file when executed and dependencies are configured"() {
        given: "a project with dependencies defined in build.gradle"
        buildFile << """
        dependencies {
            paket {
                nuget "Wooga.Test"
            }
        }
        """.stripIndent()
        and: "optional created paket.dependencies file"
        def dependenciesFile = new File(projectDir, 'paket.dependencies')
        if (createDependenciesFile) {
            dependenciesFile << """
            //empty dependencies file
            """
        }

        when:
        def result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        then:
        !result.wasSkipped(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        dependenciesFile.exists()
        !dependenciesFile.text.contains("//empty dependencies file")

        where:
        createDependenciesFile << [true, false]
        method << ["overrides", "creates"]
    }

    def ":paketDependencies skips when no internal dependencies are configured"() {
        expect:
        runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME).wasSkipped(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
    }

    def ":paketDependencies is upToData when internal dependencies are configured and run multiple times"() {
        given: "a project with dependencies defined in build.gradle"
        buildFile << """
        dependencies {
            paket {
                nuget "Wooga.Test"
            }
        }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        then:
        result.wasExecuted(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        !result.wasUpToDate(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        when:
        result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        then:
        result.wasUpToDate(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
    }

    @Unroll
    def ":paketDependencies runs again after #type"() {
        given: "a project with dependencies defined in build.gradle"

        buildFile << """
        dependencies {
            paket {
                nuget "Wooga.Test"
            }
        }
        
        repositories {
            nuget { 
                name 'test'
                url 'some/url'
            } 
        }
        """.stripIndent()

        and: "a gradle run to make task UP-TO-DATE"
        def result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        assert result.wasExecuted(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        assert !result.wasUpToDate(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        assert result.wasUpToDate(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        when: "reconfigure"
        buildFile << """
        ${reconfigure}
        """.stripIndent()

        result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        then:
        result.wasExecuted(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        !result.wasUpToDate(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        where:
        type                   | reconfigure
        "add a dependency"     | "dependencies.paket { nuget 'Wooga.Test2' }"
        "replace a dependency" | "dependencies.paket.clear(); dependencies.paket { nuget 'Wooga.Test2' }"
        "add a source"         | "repositories { nuget { name 'foo'; url 'some/url' } }"
        "replace a source"     | "repositories.clear(); repositories.nuget { url 'url' }"
        "add a macro"          | "dependencies.paket { framework 'net35' }"
        "replace a macro"      | "dependencies.paket { frameworks=['net40'] }"

    }

    def "fails creating group when group identifier is not valid"() {
        given: "a project with dependencies defined in build.gradle"

        buildFile << """
        dependencies {
            paket {
                1group {
                    nuget "Wooga.Test"
                }
            }
        }
        """.stripIndent()

        expect:
        def result = runTasksWithFailure(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)
        outputContains(result, "No signature of method")
    }

    @Unroll
    def "can retrieve and configure #groupName group"() {
        given: "a project with dependencies defined in build.gradle"

        buildFile << """
        
        def testGroup = dependencies.paket['${groupName}']
        testGroup.configure {
            nuget "Wooga.Test"
        }
        
        """.stripIndent()

        and: "a future paket.dependencies file"
        File paketDependencies = new File(projectDir, 'paket.dependencies')
        assert !paketDependencies.exists()

        when:
        def result = runTasksSuccessfully(PaketBasePlugin.PAKET_DEPENDENCIES_TASK_NAME)

        then:
        paketDependencies.exists()
        paketDependencies.text.contains("nuget Wooga.Test")

        where:
        groupName << ["custom","main"]
    }
}

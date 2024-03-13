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

import wooga.gradle.paket.FrameworkRestriction
import wooga.gradle.paket.base.utils.PaketDependenciesSpec
import wooga.gradle.paket.unity.PaketUnityPluginConventions

/**
 * Object representation of a `paket.dependencies` file.
 * https://fsprojects.github.io/Paket/dependencies-file.html
 */
class PaketDependencies implements PaketDependenciesSpec {

    PaketDependencies(File dependenciesFile) {
        this(dependenciesFile.text)
    }

    private Set<PaketDependencyDeclaration> dependencies = []
    private Set<String> sources = []
    private List<String> frameworks = []

    Set<String> getSources() {
        sources
    }

    Set<String> getNugetDependencies() {
        dependencies.collect({it.name})
    }

    List<String> getFrameworks() {
        if (frameworks == null ||  frameworks.empty){
            return PaketUnityPluginConventions.defaultFrameworks
        }
        frameworks
    }

    PaketDependencies(String dependenciesContent) {

        dependenciesContent.eachLine { line ->
            def matcher
            if ((matcher = line =~ /^\s*([\w:]+)\s([\w\/:.(, )+]+)(.*)$/)) {

                // First component
                def key = matcher[0][1].toString()
                // Second component
                def value = matcher[0][2].toString().trim()

                switch (key) {
                    case "nuget":
                        // Third component
                        def rule = matcher[0][3].toString().trim()
                        dependencies.add(new PaketDependencyDeclaration(value, rule, "nuget"))
                        break
                    case "source":
                        sources.add(value)
                        break
                    case "framework:":
                        frameworks = value.split(",").collect { it.trim() }
                        break
                }
            }
        }
    }

    PaketDependencies(){
    }

    @Override
    String toString() {
        def builder = new StringJoiner(System.lineSeparator())

        sources.forEach({
            builder.add("source ${it}")
        })

        if (!frameworks.empty){
            builder.add("framework: ${frameworks.join(',')}")
        }

        dependencies.forEach({
            builder.add("${it.source} ${it.name} ${it.rule}")
        })

        builder.toString().trim()
    }

    PaketDependencies withSource(String name) {
        sources.add(name)
        this
    }

    PaketDependencies withNugetSource(int version = 2) {
        withSource("https://nuget.org/api/v${version}")
    }

    PaketDependencies withDependency(String name, String rule, String source = "nuget") {
        dependencies.add(new PaketDependencyDeclaration(name, rule, source))
        this
    }

    PaketDependencies withFrameworks(String... names) {
        frameworks.addAll(names)
        this
    }

    PaketDependencies withFrameworks(FrameworkRestriction... values) {
        frameworks.addAll(values.collect({it.label}))
        this
    }

    List<String> getReferences(){

    }




}

/**
 * A declaration for a paket dependency, in a `paket.dependencies` file.
 * Such as {@code nuget Pancakes > 1.0}.
 */
class PaketDependencyDeclaration {
    String source
    String name
    String rule
    PaketDependencyDeclaration(String name, String rule, String source) {
        this.name = name
        this.rule = rule
        this.source = source
    }
}

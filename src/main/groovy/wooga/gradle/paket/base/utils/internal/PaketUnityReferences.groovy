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


import java.util.logging.Logger

/**
 * A single reference for an Unity WDK package
 */
class PaketUnityReference {

    private static final Logger logger = Logger.getLogger(PaketUnityReference.class.name)

    String name
    Boolean includeTests = false
    Boolean includeAssemblies = false

    PaketUnityReference(String name) {
        this.name = name
    }

    static PaketUnityReference Parse(String text) {

        String pattern = /\s?(?<name>[A-Za-z0-9\.]+)\s*?(\s+(\[(?<properties>.*?)\]))?$/

        def matcher = text =~ pattern
        if (matcher.count == 0) {
            logger.warning("Could not parse a reference from ${text}")
            return null
        }

        def (_, String name, __, ___, String properties) = matcher[0]
        PaketUnityReference result = new PaketUnityReference(name)

        if (properties) {
            try {
                def map = parseMap(properties)
                if (map) {
                    result.includeTests = Boolean.parseBoolean(map["includeTests"]) ?: false
                    result.includeAssemblies = Boolean.parseBoolean(map["includeAssemblies"]) ?: false
                }
            }
            catch (Exception e) {
                logger.warning("Failed to parse map from ${text}:\n${e}")
            }
        }

        result
    }

    /***
     * @param input A String containing a groovy map in the format "key:value,key:value"
     * @return A map containing the parsed string's key-value pairs
     */
    static Map<String, ?> parseMap(String input) {
        (input =~ /\s*,?\s*([^:]+?)\s*:\s*([^,]*)\s*/)
                .collect { [it[1], it[2].trim()] }
                .collectEntries()
    }
}

/**
 * A parsed <i>paket.unity3d.references</i> file, which contains
 * references to the dependencies used by a project.
 * These are used by this plugin during a paketInstall invocation
 */
class PaketUnityReferences {

    private final Map<String, PaketUnityReference> referencesByName
    private final List<PaketUnityReference> references

    List<PaketUnityReference> getReferences() {
        references
    }

    List<String> getReferenceNames() {
        references.collect({it -> it.name})
    }

    PaketUnityReferences(File referencesFile) {
        this(referencesFile.text)
    }

    PaketUnityReferences(String referencesContent) {
        references = []
        referencesByName = [:]

        referencesContent.eachLine { line ->
            if (!line.empty) {
                PaketUnityReference ref = PaketUnityReference.Parse(line.trim())
                if (ref != null) {
                    references << ref
                    referencesByName.put(ref.name, ref)
                }
            }
        }
    }

    Boolean containsReference(String name) {
        referencesByName.containsKey(name)
    }

    PaketUnityReference getReference(String name) {
        referencesByName.containsKey(name) ? referencesByName[name] : null
    }
}

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

/**
 * The parsed contents of paket.lock file
 */
class PaketLock {

    enum SourceType {
        NUGET("NUGET")

        private final String value

        SourceType(String value) {
            this.value = value
        }

        String getValue() {
            value
        }
    }

    enum LineType {

        TYPE(0),
        REMOTE(1),
        NAME(2),
        DEPENDENCY(3)

        private final int value

        LineType(int value) {
            this.value = value
        }

        int getValue() {
            value
        }
    }

    private def content

    PaketLock(File lockFile) {
        this(lockFile.text.stripIndent())
    }

    PaketLock(String lockContent) {
        content = [:]

        String currentSourceType
        String currentPackageName
        int currentIndent = 0
        String currentLineData

        lockContent.eachLine { line ->

            currentLineData = line.trim()
            if (currentLineData.trim().empty) {
                return
            }

            def match = (line =~ /^[\s]+/)
            currentIndent = !match ? 0 : match[0].size() / 2

            switch (currentIndent) {
                case LineType.TYPE.value:
                    if(isValidSourceType(currentLineData)) {
                        currentSourceType = currentLineData
                        content[currentSourceType] = [:]
                    }
                    break
                case LineType.NAME.value:
                    currentPackageName = currentLineData.split(/\s/)[0]
                    if(currentSourceType && currentPackageName) {
                        content[currentSourceType][currentPackageName] = []
                    }
                    break

                case LineType.DEPENDENCY.value:
                    if(currentSourceType && currentPackageName) {
                        (content[currentSourceType][currentPackageName] as List<String>) << currentLineData.split(/\s/)[0]
                    }
                    break

                case LineType.REMOTE.value:
                default:
                    break
            }
        }
    }

    static boolean isValidSourceType(String value) {

        for (SourceType type in SourceType.values()) {
            if (type.value == value) return true
        }
        return false
    }

    /**
     * @param id The identifier for the dependency
     * @return All the direct and transitive dependencies used by this dependency
     */
    List<String> getDependencies(SourceType source, String id) {
        // If the lock file has a reference with the given id,
        // retrieve all its dependencies
        content[source.value] && content[source.value][id]
                ? content[source.value][id] as List<String>
                : []
    }

    /**
     * @return Given a list of package references, returns all their dependencies
     */
    List<String> getAllDependencies(List<String> references) {
        def result = references.collect { reference ->
            [reference, getAllDependencies(getDependencies(SourceType.NUGET, reference))]
        }
        result.flatten().unique()
    }
}

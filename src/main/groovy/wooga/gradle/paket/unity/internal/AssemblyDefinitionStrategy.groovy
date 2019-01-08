/*
 * Copyright 2019 Wooga GmbH
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

package wooga.gradle.paket.unity.internal


import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

abstract class AssemblyDefinitionStrategy implements wooga.gradle.paket.unity.AssemblyDefinitionStrategy {

    private static final Logger BUILD_LOGGER = Logging.getLogger(AssemblyDefinitionStrategy.class)

    abstract void execute(File installDirectory, Map<String, Set<String>> tree, Set<File> references, Set<File> editorReferences)

    Logger getLogger() {
        BUILD_LOGGER
    }

    static File assemblyDefinitionPathForDirectory(File directory) {
        AssemblyDefinition.assemblyDefinitionPathForDirectory(directory)
    }

    static File assemblyDefinitionPathForDirectory(File directory, String postFix) {
        AssemblyDefinition.assemblyDefinitionPathForDirectory(directory, postFix)
    }
}

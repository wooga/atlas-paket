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

import groovy.json.JsonOutput
import org.apache.commons.io.FilenameUtils

import java.beans.Transient

class AssemblyDefinition {

    private class Model {
        String name
        List<String> references
        List<String> optionalUnityReferences
        List<String> includePlatforms
        List<String> excludePlatforms
        boolean allowUnsafeCode
        boolean overrideReferences
        boolean autoReferenced
        List<String> precompiledReferences
        List<String> defineConstraints
    }

    @Delegate
    final Model model
    final private File filePath

    @Transient
    File getFilePath() {
        filePath
    }

    AssemblyDefinition(File path) {
        this(path, true)
    }

    AssemblyDefinition(File path, List<String> references) {
        this(path, true, references)
    }

    AssemblyDefinition(File path, boolean autoReferenced) {
        this(path, autoReferenced, [])
    }

    AssemblyDefinition(File path, boolean autoReferenced, List<String> references) {
        this.filePath = path
        this.model = new Model()

        this.name = FilenameUtils.getBaseName(path.name)
        this.autoReferenced = autoReferenced

        allowUnsafeCode = false
        overrideReferences = false

        this.references = []
        this.references.addAll(references)
        optionalUnityReferences = []
        includePlatforms = []
        excludePlatforms = []
        precompiledReferences = []
        defineConstraints = []
    }

    void export() {
        export(this)
    }

    static void export(AssemblyDefinition definition) {
        def output = JsonOutput.toJson(definition.model)
        definition.filePath.text = JsonOutput.prettyPrint(output)
    }

    static File assemblyDefinitionPathForDirectory(File directory) {
        assemblyDefinitionPathForDirectory(directory, null)
    }

    static File assemblyDefinitionPathForDirectory(File directory, String postFix) {
        def name = directory.name
        if(postFix) {
            name += ".${postFix}"
        }
        new File(directory, "${name}.asmdef")
    }
}

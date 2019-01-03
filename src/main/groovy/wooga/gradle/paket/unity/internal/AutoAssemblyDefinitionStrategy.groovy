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

class AutoAssemblyDefinitionStrategy extends AssemblyDefinitionStrategy {
    @Override
    void execute(File installDirectory) {
        logger.info("execute auto assembly definition strategy")

        List<AssemblyDefinition> definitions = []
        AssemblyDefinition baseDefinition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(installDirectory))
        definitions << baseDefinition

        def editorDefinitionAction = { File editorDir ->
            logger.debug("found Editor directory in install dir")
            AssemblyDefinition definition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(editorDir), [baseDefinition.name])

            definition.name = generateDefinitionName(installDirectory, editorDir)
            definition.includePlatforms << "Editor"
            logger.debug("generate assembly definition file at: ${definition.filePath.path}")
            definitions << definition
        }

        installDirectory.eachDirMatch("Editor", editorDefinitionAction)
        installDirectory.eachDirRecurse { File dir ->
            dir.eachDirMatch("Editor", editorDefinitionAction)
        }

        logger.debug("export assembly definition files")
        definitions.each { definition ->
            def output = JsonOutput.toJson(definition.model)
            definition.filePath.text = JsonOutput.prettyPrint(output)
        }
    }

    private static String generateDefinitionName(File installDirectory, File directory) {
        def dirUri = directory.toURI()
        def installDirUri = installDirectory.parentFile.toURI()
        def relativeDirUri = installDirUri.relativize(dirUri)

        relativeDirUri.toString().split('/').collect({it.capitalize()}).join()
    }
}

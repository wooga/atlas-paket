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
import groovy.json.JsonSlurper

class AutoAssemblyDefinitionStrategy extends AssemblyDefinitionStrategy {
    @Override
    void execute(File installDirectory, Map<String, Set<String>> tree, Set<File> references, Set<File> editorReferences) {
        logger.info("execute auto assembly definition strategy")

        List<AssemblyDefinition> definitions = []

//        AssemblyDefinition baseDefinition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(installDirectory))
//        definitions << baseDefinition
//
//        def editorDefinitions = [:]
//
//        tree.each { String dependency, Set<String> refs ->
//            File dep = new File(installDirectory, dependency)
//            AssemblyDefinition definition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(dep))
//            definition.references.addAll(refs)
//            editorDefinitions[dependency] = []
//
//            def editorDefinitionAction = { File editorDir ->
//                logger.debug("found Editor directory in install dir")
//                String definitionName = generateDefinitionName(installDirectory, editorDir)
//                File definitionPath = new File(editorDir, "${definitionName}.asmdef")
//
//                AssemblyDefinition editorDefinition = new AssemblyDefinition(definitionPath, [dependency, *refs.toArray()])
//
//                editorDefinition.name = definitionName
//                editorDefinition.includePlatforms << "Editor"
//                logger.debug("generate assembly definition file at: ${editorDefinition.filePath.path}")
//                definitions << editorDefinition
//                editorDefinitions[dependency] << editorDefinition
//            }
//
//            dep.eachDirMatch("Editor", editorDefinitionAction)
//            dep.eachDirRecurse { File dir ->
//                dir.eachDirMatch("Editor", editorDefinitionAction)
//            }
//
//            definitions << definition
//            baseDefinition.references << definition.name
//        }
//
//        tree.each { String dependency, Set<String> refs ->
//            def depEditorDefinitions = editorDefinitions[dependency]
//            depEditorDefinitions.each { AssemblyDefinition definition ->
//                definition.references.addAll(refs.collect({editorDefinitions[it]}).flatten().collect({it.name}).unique())
//            }
//        }

        AssemblyDefinition baseDefinition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(installDirectory))
        definitions << baseDefinition

        def editorDefinitions = [:]

        tree.each { String dependency, Set<String> refs ->
            File dep = new File(installDirectory, dependency)
            editorDefinitions[dependency] = []

            def editorDefinitionAction = { File editorDir ->
                logger.debug("found Editor directory in install dir")
                String definitionName = generateDefinitionName(installDirectory, editorDir)
                File definitionPath = new File(editorDir, "${definitionName}.asmdef")

                AssemblyDefinition editorDefinition = new AssemblyDefinition(definitionPath, [baseDefinition.name])

                editorDefinition.name = definitionName
                editorDefinition.includePlatforms << "Editor"
                logger.debug("generate assembly definition file at: ${editorDefinition.filePath.path}")
                definitions << editorDefinition
                editorDefinitions[dependency] << editorDefinition
            }

            dep.eachDirMatch("Editor", editorDefinitionAction)
            dep.eachDirRecurse { File dir ->
                dir.eachDirMatch("Editor", editorDefinitionAction)
            }
        }

        tree.each { String dependency, Set<String> refs ->
            def depEditorDefinitions = editorDefinitions[dependency]
            depEditorDefinitions.each { AssemblyDefinition definition ->
                definition.references.addAll(refs.collect({editorDefinitions[it]}).flatten().collect({it.name}).unique())
            }
        }


//        List<AssemblyDefinition> editorDefinitions = []
//        AssemblyDefinition baseDefinition = new AssemblyDefinition(assemblyDefinitionPathForDirectory(installDirectory))
//        definitions << baseDefinition
//
//        def editorDefinitionAction = { File editorDir ->
//            logger.debug("found Editor directory in install dir")
//            String definitionName = generateDefinitionName(installDirectory, editorDir)
//            File definitionPath = new File(editorDir, "${definitionName}.asmdef")
//
//            AssemblyDefinition definition = new AssemblyDefinition(definitionPath, [baseDefinition.name])
//
//            definition.name = definitionName
//            definition.includePlatforms << "Editor"
//            logger.debug("generate assembly definition file at: ${definition.filePath.path}")
//            definitions << definition
//            editorDefinitions << definition
//        }
//
//        installDirectory.eachDirMatch("Editor", editorDefinitionAction)
//        installDirectory.eachDirRecurse { File dir ->
//            dir.eachDirMatch("Editor", editorDefinitionAction)
//        }
//
//        logger.info("cross reference all Editor assemblies")
//        editorDefinitions.each { definition ->
//            definition.references.addAll(editorDefinitions.findAll({it.name != definition.name}).collect({it.name}))
//        }
//
        logger.info("export assembly definition files")
        definitions.each { definition ->
            logger.info("export ${definition.name} to ${definition.filePath}")
            def output = JsonOutput.toJson(definition.model)
            definition.filePath.text = JsonOutput.prettyPrint(output)
        }

        logger.info("reference installed definition")
        references.each {
            logger.info("add reference to ${it}")
            def definition = readDefinition(it)
            def refs = definition["references"]
            refs = (refs) ? (refs as List<String>).toSet() : [].toSet()

            refs.add(baseDefinition.name)
            definition["references"] = refs
            it.text = JsonOutput.prettyPrint(JsonOutput.toJson(definition))
        }

        editorReferences.each {
            logger.info("add editor reference to ${it}")
            def definition = readDefinition(it)
            def refs = definition["references"]
            refs = (refs) ? (refs as List<String>).toSet() : [].toSet()
            refs.addAll(editorDefinitions.values().flatten().toSet().collect({it.name}))
            definition["references"] = refs
            it.text = JsonOutput.prettyPrint(JsonOutput.toJson(definition))
        }
    }

    def readDefinition(File file) {
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parse(file)
    }

    private static String generateDefinitionName(File installDirectory, File directory) {
        def dirUri = directory.toURI()
        def installDirUri = installDirectory.toURI()
        def relativeDirUri = installDirUri.relativize(dirUri)

        relativeDirUri.toString().split('/').collect({it.capitalize().split(/\./)}).flatten().unique().join(".")
    }
}

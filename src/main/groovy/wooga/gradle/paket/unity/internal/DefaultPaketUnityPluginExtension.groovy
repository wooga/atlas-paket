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

package wooga.gradle.paket.unity.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.dependencies.PaketDependencyHandler
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.unity.PaketUnityPluginExtension

class DefaultPaketUnityPluginExtension extends DefaultPaketPluginExtension implements PaketUnityPluginExtension {

    public static final String DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME = "paket.unity3d.references"
    public static final String DEFAULT_PAKET_DIRECTORY = "Paket.Unity3D"

    protected final List<File> installDefinitions
    protected final List<File> editorInstallDefinitions

    protected AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy
    protected String customPaketOutputDirectory

    DefaultPaketUnityPluginExtension(Project project,final PaketDependencyHandler dependencyHandler) {
        super(project, dependencyHandler)
        assemblyDefinitionFileStrategy = AssemblyDefinitionFileStrategy.manual
        installDefinitions = []
        editorInstallDefinitions = []
    }

    @Override
    FileCollection getPaketReferencesFiles() {
        project.files(project.fileTree(dir: project.projectDir, include: "**/${DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}").files)
    }


    String getPaketOutputDirectoryName() {
        return customPaketOutputDirectory ?: DEFAULT_PAKET_DIRECTORY
    }


    void setPaketOutputDirectoryName(String directory) {
        customPaketOutputDirectory = directory
    }

    @Override
    AssemblyDefinitionFileStrategy getAssemblyDefinitionFileStrategy() {
        return assemblyDefinitionFileStrategy
    }

    @Override
    void setAssemblyDefinitionFileStrategy(AssemblyDefinitionFileStrategy strategy) {
        assemblyDefinitionFileStrategy = strategy
    }

    @Override
    void referenceAssemblyDefintionFilesIn(File pathToDefinition) {
        installDefinitions.add(pathToDefinition)
    }

    @Override
    void referenceAssemblyDefintionFilesIn(FileCollection definitions) {
        installDefinitions.addAll(definitions.files)
    }

    @Override
    List<File> getAssemblyDefinitionFilesToAddReferences() {
        installDefinitions.collect()
    }

    @Override
    void setAssemblyDefinitionFilesToAddReferences(List<File> definitions) {
        installDefinitions.removeAll()
        installDefinitions.addAll(definitions)
    }

    @Override
    void referenceEditorAssemblyDefintionFilesIn(File definition) {
        editorInstallDefinitions.add(definition)
    }

    @Override
    void referenceEditorAssemblyDefintionFilesIn(FileCollection definitions) {
        editorInstallDefinitions.addAll(definitions.files)
    }

    @Override
    List<File> getEditorAssemblyDefinitionFilesToAddReferences() {
        return editorInstallDefinitions.collect()
    }

    @Override
    void setEditorAssemblyDefinitionFilesToAddReferences(List<File> definitions) {
        editorInstallDefinitions.removeAll()
        editorInstallDefinitions.addAll(definitions)
    }
}

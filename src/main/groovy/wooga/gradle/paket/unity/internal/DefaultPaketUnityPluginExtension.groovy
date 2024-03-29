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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import wooga.gradle.paket.base.dependencies.PaketDependencyHandler
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.unity.PaketUnityPluginExtension

class DefaultPaketUnityPluginExtension extends DefaultPaketPluginExtension implements PaketUnityPluginExtension {

    public static final String DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME = "paket.unity3d.references"
    public static final String DEFAULT_PAKET_DIRECTORY = "Paket.Unity3D"
    //This path is relative to the <unityProject>/Assets dir
    public static final String UNITY_PACKAGES_DIRECTORY = "../Packages"

    protected String customPaketOutputDirectory
    protected Boolean includeAssemblyDefinitions = false
    protected ListProperty<String> preInstalledUpmPackages

    DefaultPaketUnityPluginExtension(Project project,final PaketDependencyHandler dependencyHandler) {
        super(project, dependencyHandler)
        preInstalledUpmPackages = project.objects.listProperty(String)
    }

    @Override
    FileCollection getPaketReferencesFiles() {
        project.files(project.fileTree(dir: project.projectDir, include: "**/${DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}").files)
    }

    String getPaketOutputDirectoryName() {
        if(paketUpmPackageEnabled.get()) {
            return customPaketOutputDirectory ?: UNITY_PACKAGES_DIRECTORY
        }
        return customPaketOutputDirectory ?: DEFAULT_PAKET_DIRECTORY
    }


    void setPaketOutputDirectoryName(String directory) {
        customPaketOutputDirectory = directory
    }

    @Override
    void setIncludeAssemblyDefinitions(Boolean value) {
        includeAssemblyDefinitions = value
    }

    @Override
    Boolean getIncludeAssemblyDefinitions() {
        includeAssemblyDefinitions
    }

    @Override
    List<String> getPreInstalledUpmPackages() {
        preInstalledUpmPackages.getOrElse([])
    }

    @Override
    void setPreInstalledUpmPackages(List<String> value) {
        preInstalledUpmPackages.set(value)
    }

    @Override
    void setPreInstalledUpmPackages(Provider<List<String>> value) {
        preInstalledUpmPackages.set(value)
    }

    @Override
    Provider<List<String>> getPreInstalledUpmPackagesProvider() {
        preInstalledUpmPackages
    }


}

/*
 * Copyright 2017 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.unity

import org.gradle.api.Project
import wooga.gradle.paket.base.DefaultPaketPluginExtension

class DefaultPaketUnityPluginExtension extends DefaultPaketPluginExtension {

    private static final String DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME = "**/paket.unity3d.references"
    private static final String DEFAULT_PAKET_UNITY_EXECUTION_NAME = "paket.unity3d.exe"
    private static final String DEFAULT_PAKET_UNITY_BOOTSTRAPPER_EXECUTION_NAME = "paket.unity3d.bootstrapper.exe"
    private static final String DEFAULT_PAKET_UNITY_BOOTSTRAPPER_URL = "https://github.com/wooga/Paket.Unity3D/releases/download/0.2.1/paket.unity3d.bootstrapper.exe"

    DefaultPaketUnityPluginExtension(Project project) {
        super(project)
    }

    @Override
    String getPaketBootstrapperUrl() {
        return customPaketBootstrapperUrl ?: DEFAULT_PAKET_UNITY_BOOTSTRAPPER_URL
    }

    @Override
    protected String getExecutableName() {
        DEFAULT_PAKET_UNITY_EXECUTION_NAME
    }

    @Override
    protected String getBootstrapperExecutableName() {
        DEFAULT_PAKET_UNITY_BOOTSTRAPPER_EXECUTION_NAME
    }

    @Override
    File getPaketDependenciesFile() {
        def files = project.files(project.fileTree(dir: project.projectDir, include: DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME).files)
        files.isEmpty() ? new File(project.projectDir, "no-source") : files.first()
    }
}

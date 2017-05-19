/*
 * Copyright 2017 the original author or authors.
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

import wooga.gradle.paket.base.PaketPluginExtension

class DefaultPaketUnityPluginExtension implements PaketPluginExtension {
    String paketExecuteableName = "paket.unity3d.exe"
    String paketBootstrapperFileName = "paket.unity3d.bootstrapper.exe"
    String paketBootstrapperDownloadURL = "https://github.com/wooga/Paket.Unity3D/releases/download/0.2.1/paket.unity3d.bootstrapper.exe"
}

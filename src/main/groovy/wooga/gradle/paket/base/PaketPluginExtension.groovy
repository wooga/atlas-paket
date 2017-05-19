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

package wooga.gradle.paket.base

class PaketPluginExtension {

    String paketDirectory = ".paket"

    String paketExecuteableName = "paket.exe"
    String paketBootstrapperFileName = "paket.bootstrapper.exe"
    String paketBootstrapperDownloadURL = "https://github.com/fsprojects/Paket/releases/download/4.8.5/paket.bootstrapper.exe"

    String version = ""
    String monoExecutable = "mono"

    PaketPluginExtension(boolean unity = false) {
        if(unity)
        {
            paketExecuteableName = "paket.unity3d.exe"
            paketBootstrapperFileName = "paket.unity3d.bootstrapper.exe"
            paketBootstrapperDownloadURL = "https://github.com/wooga/Paket.Unity3D/releases/download/0.2.1/paket.unity3d.bootstrapper.exe"
        }
    }

    String getPaketExecuteablePath() {
        "$paketDirectory/$paketExecuteableName"
    }

    String getPaketBootstrapperPath() {
        "$paketDirectory/$paketBootstrapperFileName"
    }
}

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

package wooga.gradle.paket.base

interface PaketPluginExtension {

    File getPaketDirectory()
    void setPaketDirectory(Object directory)
    PaketPluginExtension paketDirectory(Object directory)
    
    String getVersion()
    void setVersion(Object version)
    PaketPluginExtension version(Object version)

    String getMonoExecutable()
    void setMonoExecutable(Object path)
    PaketPluginExtension monoExecutable(Object path)

    File getExecutable()
    void setExecutable(Object executable)
    PaketPluginExtension executable(Object executable)

    File getBootstrapperExecutable()
    void setBootstrapperExecutable(Object fileName)
    PaketPluginExtension bootstrapperExecutable(Object fileName)

    String getPaketBootstrapperUrl()
    void setPaketBootstrapperUrl(Object url)
    PaketPluginExtension paketBootstrapperUrl(Object url)

    File getPaketDependenciesFile()

}

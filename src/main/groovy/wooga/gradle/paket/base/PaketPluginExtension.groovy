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

import wooga.gradle.paket.base.utils.PaketDependencies

/**
 * Base extension point for paket plugins.
 */
interface PaketPluginExtension {

    /**
     * Returns the {@File} path to the paket directory.
     * <p>
     * The {@code .paket} is the place the plugin or the project stores the {@code paket.exe}
     * and {@code paket.bootstrapper.exe} executables.
     *
     * @return  the path to {@code paket} directory.
     * @default {@code projectDir/.paket}
     */
    File getPaketDirectory()

    /**
     * Sets the path to the paket directory.
     * <p>
     * The value can be anything that can be converted into a File/file path string.
     * @param   directory the path to the paket directory
     */
    void setPaketDirectory(Object directory)

    /**
     * Sets the path to the paket directory.
     * <p>
     * The value can be anything that can be converted into a File/file path string.
     * @param   directory the path to the paket directory
     * @return  this
     */
    PaketPluginExtension paketDirectory(Object directory)

    /**
     * Returns the paket version to use.
     * <p>
     * This version will be used to load a specific version of the paket binary.
     * When empty, the bootstrapper will load the latest version available.
     *
     * @return  the version for paket.
     * @default ""
     */
    String getVersion()

    /**
     * Sets version for the paket binary.
     * <p>
     * There is no version validation implemented.
     * The behavior is undefined for invalid input.
     *
     * @param   version the version string
     */
    void setVersion(Object version)

    /**
     * Sets version for the paket binary.
     * <p>
     * There is no version validation implemented.
     * The behavior is undefined for invalid input.
     *
     * @param   version the version string
     * @returns this
     */
    PaketPluginExtension version(Object version)

    /**
     * Returns the file path to mono executable as String.
     * <p>
     * This value won't be used on windows.
     *
     * @return  the path to mono executable
     * @default "mono"
     */
    String getMonoExecutable()

    /**
     * Sets the path to mono executable
     *
     * @param   path to mono
     */
    void setMonoExecutable(Object path)

    /**
     * Sets the path to mono executable
     *
     * @param   path to mono
     * @returns this
     */
    PaketPluginExtension monoExecutable(Object path)

    /**
     * Returns the path to the paket tool executable
     *
     * @return executable path
     */
    File getExecutable()

    /**
     * Sets the paket executable path
     *
     * @param   executable
     */
    void setExecutable(Object executable)

    /**
     * Sets the paket executable path
     *
     * @param   executable
     * @returns this
     */
    PaketPluginExtension executable(Object executable)

    /**
     * Returns the path to the bootstrapper executable
     *
     * @return  path to bootstrapper executable
     */
    File getBootstrapperExecutable()

    /**
     * Sets the boostrapper executable path
     *
     * @param   fileName
     */
    void setBootstrapperExecutable(Object fileName)

    /**
     * Sets the boostrapper executable path
     *
     * @param   fileName
     * @return  this
     */
    PaketPluginExtension bootstrapperExecutable(Object fileName)

    /**
     * Returns an URL string to the bootstrapper download location
     *
     * @return the bootstrappere download URL
     */
    String getPaketBootstrapperUrl()

    /**
     * Sets the bootstrapper download URL
     *
     * @param   url
     */
    void setPaketBootstrapperUrl(Object url)

    /**
     * Sets the bootstrapper download URL
     *
     * @param   url
     * @returns this
     */
    PaketPluginExtension paketBootstrapperUrl(Object url)

    /**
     * Returns the {@link File} path to the {@code paket.dependencies} file within the project.
     *
     * @return path to {@code paket.dependencies} file.
     */
    File getPaketDependenciesFile()

    /**
     * Returns the content of {@code paket.dependencies} parsed as {@link PaketDependencies} object.
     *
     * @return  the parsed {@code paket.dependencies} file
     * @see     PaketDependencies
     */
    PaketDependencies getPaketDependencies()
}

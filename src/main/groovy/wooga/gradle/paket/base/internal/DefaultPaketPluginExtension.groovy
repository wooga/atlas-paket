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

package wooga.gradle.paket.base.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.PaketPluginExtension
import wooga.gradle.paket.base.dependencies.PaketDependencyHandler
import wooga.gradle.paket.base.utils.internal.PaketDependencies
import wooga.gradle.paket.base.utils.internal.PaketTemplate

class DefaultPaketPluginExtension implements PaketPluginExtension {
    private static final String DEFAULT_PAKET_DIRECTORY = ".paket"
    private static final String DEFAULT_PAKET_EXECUTION_NAME = "paket.exe"
    private static final String DEFAULT_PAKET_BOOTSTRAPPER_EXECUTION_NAME = "paket.bootstrapper.exe"
    private static final String DEFAULT_PAKET_DEPENDENCIES_FILE_NAME = "paket.dependencies"
    public static final String DEFAULT_PAKET_TEMPLATE_FILE_NAME = "paket.template"
    private static
    final String DEFAULT_PAKET_BOOTSTRAPPER_URL = "https://github.com/fsprojects/Paket/releases/download/5.155.0/paket.bootstrapper.exe"

    private static final String DEFAULT_VERSION = ""
    private static final String DEFAULT_MONO_EXECUTABLE = "mono"
    private static final String DEFAULT_PAKET_LOCK_FILE_NAME = "paket.lock"

    private final PaketDependencyHandler dependencyHandler

    protected Project project
    protected File customPaketDirectory
    protected File customMonoExecutable
    protected File customPaketExecutable
    protected File customPaketBootstrapperExecutable
    protected String customVersion
    protected String customPaketBootstrapperUrl

    DefaultPaketPluginExtension(final Project project, final PaketDependencyHandler dependencyHandler) {
        this.project = project
        this.dependencyHandler = dependencyHandler
    }

    @Override
    File getPaketDirectory() {
        if (customPaketDirectory) {
            return customPaketDirectory
        }
        project.file(DEFAULT_PAKET_DIRECTORY)
    }

    @Override
    void setPaketDirectory(Object directory) {
        customPaketDirectory = project.file(directory)
    }

    @Override
    PaketPluginExtension paketDirectory(Object directory) {
        setPaketDirectory(directory)
        this
    }

    @Override
    String getVersion() {
        return customVersion ?: DEFAULT_VERSION
    }

    @Override
    void setVersion(Object version) {
        this.customVersion = version
    }

    @Override
    PaketPluginExtension version(Object version) {
        setVersion(version)
        this
    }

    @Override
    String getMonoExecutable() {
        return customMonoExecutable ?: DEFAULT_MONO_EXECUTABLE
    }

    @Override
    void setMonoExecutable(Object path) {
        this.customMonoExecutable = path
    }

    @Override
    PaketPluginExtension monoExecutable(Object path) {
        setMonoExecutable(path)
        this
    }

    @Override
    File getExecutable() {
        if (!customPaketExecutable) {
            customPaketExecutable = new File(getPaketDirectory(), getExecutableName())
        }
        customPaketExecutable
    }

    @Override
    void setExecutable(Object executable) {
        this.customPaketExecutable = project.file(executable)
    }

    @Override
    PaketPluginExtension executable(Object executable) {
        setExecutable(executable)
        this
    }

    @Override
    File getBootstrapperExecutable() {
        if (!customPaketBootstrapperExecutable) {
            customPaketBootstrapperExecutable = new File(getPaketDirectory(), getBootstrapperExecutableName())
        }
        customPaketBootstrapperExecutable
    }

    @Override
    void setBootstrapperExecutable(Object fileName) {
        this.customPaketBootstrapperExecutable = project.file(fileName)
    }

    @Override
    PaketPluginExtension bootstrapperExecutable(Object fileName) {
        setBootstrapperExecutable(fileName)
        this
    }

    @Override
    String getPaketBootstrapperUrl() {
        customPaketBootstrapperUrl ?: DEFAULT_PAKET_BOOTSTRAPPER_URL
    }

    @Override
    void setPaketBootstrapperUrl(Object url) {
        this.customPaketBootstrapperUrl = url
    }

    @Override
    PaketPluginExtension paketBootstrapperUrl(Object url) {
        setPaketBootstrapperUrl(url)
        this
    }

    @Override
    File getPaketDependenciesFile() {
        new File(project.projectDir, DEFAULT_PAKET_DEPENDENCIES_FILE_NAME)
    }

    @Override
    PaketDependencies getPaketDependencies() {
        getPaketDependenciesFile().exists() ? new PaketDependencies(getPaketDependenciesFile()) : new PaketDependencies("")
    }

    protected String getExecutableName() {
        DEFAULT_PAKET_EXECUTION_NAME
    }

    protected String getBootstrapperExecutableName() {
        DEFAULT_PAKET_BOOTSTRAPPER_EXECUTION_NAME
    }

    @Override
    File getPaketLockFile() {
        return new File(project.projectDir, DEFAULT_PAKET_LOCK_FILE_NAME)
    }

    @Override
    PaketDependencyHandler getDependencyHandler() {
        dependencyHandler
    }

    @Override
    List<File> getPaketTemplateFiles() {
        def files = project.files(project.fileTree(dir: project.projectDir, include: "**/${DEFAULT_PAKET_TEMPLATE_FILE_NAME}").files)
        return files.sort().sort(true) { o1, o2 ->
            String sep = File.separator
            if (o1.path.count(sep) > o2.path.count(sep)) {
                return 1
            } else if (o1.path.count(sep) < o2.path.count(sep)) {
                return -1
            } else {
                return 0
            }
        }
    }

    @Override
    List<PaketTemplate> getPaketTemplates()
    {
        getPaketTemplateFiles().collect {new PaketTemplate(it)}
    }
}

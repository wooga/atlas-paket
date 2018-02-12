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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.base.PaketPluginExtension
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.get.tasks.PaketUpdate
import wooga.gradle.paket.unity.tasks.PaketUnityBootstrap
import wooga.gradle.paket.unity.tasks.PaketUnityInstall

class PaketUnityPlugin implements Plugin<Project> {

    Project project

    static final String GROUP = "PaketUnity"
    static final String EXTENSION_NAME = 'paketUnity'
    static final String BOOTSTRAP_TASK_NAME = "paketUnityBootstrap"
    static final String INSTALL_TASK_NAME = "paketUnityInstall"

    @Override
    void apply(Project project) {
        this.project = project

        project.pluginManager.apply(PaketBasePlugin.class)

        final extension = project.extensions.create(EXTENSION_NAME, DefaultPaketUnityPluginExtension, project)
        final paketUnityBootstrap = createPaketUnityBootstrapTask(project, extension)
        final paketInstall = createPaketUnityInstallTask(project, extension)

        project.tasks.matching({ it.name.startsWith("paketUnity") }).each { task ->
            task.onlyIf {
                project.file("$project.projectDir/${PaketBasePlugin.DEPENDENCIES_FILE_NAME}").exists()
            }
        }

        paketInstall.dependsOn paketUnityBootstrap

        configurePaketDependencyInstallIfPresent()
    }

    private static Task createPaketUnityInstallTask(final Project project, final PaketPluginExtension extension) {
        def task = project.tasks.create(INSTALL_TASK_NAME, PaketUnityInstall.class)
        def conventionMapping = task.conventionMapping
        conventionMapping.map("executable", { extension.getExecutable() })

        task.setDependenciesFile(DependenciesFileCollection(project, extension))

        task.group = GROUP
        task
    }

    private static Task createPaketUnityBootstrapTask(final Project project, final PaketPluginExtension extension) {

        def task = project.tasks.create(BOOTSTRAP_TASK_NAME, PaketUnityBootstrap.class)
        def conventionMapping = task.conventionMapping
        task.setExecutable(extension.getBootstrapperExecutable())
        task.setBootstrapURL(extension.getPaketBootstrapperUrl())
        task.setPaketVersion(extension.getVersion())
        /*
        task.setDependenciesFile(DependenciesFileCollection(project, extension))
        conventionMapping.map("executable", { extension.getBootstrapperExecutable() })
        conventionMapping.map("bootstrapURL", { extension.getPaketBootstrapperUrl() })
        conventionMapping.map("paketVersion", { extension.getVersion() })
        */
        conventionMapping.map("dependenciesFile", { DependenciesFileCollection(project, extension) })

        task
    }

    private static FileCollection DependenciesFileCollection(Project project, PaketPluginExtension extension) {
        extension.getPaketDependenciesFile().exists() ? project.files(extension.getPaketDependenciesFile()) : project.files()
    }

    void configurePaketDependencyInstallIfPresent() {
        project.plugins.withType(PaketGetPlugin) {

            def paketUnityInstall = project.tasks[INSTALL_TASK_NAME]
            def paketInstall = project.tasks[PaketGetPlugin.INSTALL_TASK_NAME]
            def paketRestore = project.tasks[PaketGetPlugin.RESTORE_TASK_NAME]

            Closure configClosure = { task ->
                task.finalizedBy paketUnityInstall
            }

            project.tasks.withType(PaketUpdate, configClosure)

            [paketInstall, paketRestore].each configClosure
        }
    }
}

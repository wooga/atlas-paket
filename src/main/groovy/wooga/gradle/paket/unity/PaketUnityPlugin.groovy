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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.tasks.PaketUnityBootstrap
import wooga.gradle.paket.unity.tasks.PaketUnityInstall

class PaketUnityPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String GROUP = "PaketUnity"
    static final String EXTENSION_NAME = 'paketUnity'
    static final String BOOTSTRAP_TASK_NAME = "paketUnityBootstrap"
    static final String INSTALL_TASK_NAME = "paketUnityInstall"

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        def extension = project.extensions.create(EXTENSION_NAME, DefaultPaketUnityPluginExtension)

        def paketBootstrap = tasks.create(BOOTSTRAP_TASK_NAME, PaketUnityBootstrap.class)
        paketBootstrap.outputDir = { "$project.projectDir/${extension.paketDirectory}" }
        paketBootstrap.paketBootstrapperFileName = { extension.paketBootstrapperFileName }
        paketBootstrap.bootstrapURL = { extension.paketBootstrapperDownloadURL }
        paketBootstrap.paketExtension = extension
        paketBootstrap.paketVersion = { extension.version }

        paketBootstrap.paketBootstrapper = { "${paketBootstrap.outputDir.path}/${extension.paketBootstrapperFileName}" }
        paketBootstrap.paketFile = { "${paketBootstrap.outputDir.path}/${extension.paketExecuteableName}" }

        def paketInstall = tasks.create(INSTALL_TASK_NAME, PaketUnityInstall.class)

        paketInstall.paketExtension = extension
        paketInstall.group = GROUP
        paketInstall.dependsOn paketBootstrap

        tasks.matching({ it.name.startsWith("paketUnity") }).each { task ->
            task.onlyIf {
                project.file("$project.projectDir/${PaketBasePlugin.DEPENDENCIES_FILE_NAME}").exists()
            }
        }

        configurePaketDependencyInstallIfPresent()
    }

    void configurePaketDependencyInstallIfPresent() {
        project.plugins.withType(PaketGetPlugin) {
            def paketInstall = project.tasks[PaketGetPlugin.INSTALL_TASK_NAME]
            def paketUpdate = project.tasks[PaketGetPlugin.UPDATE_TASK_NAME]
            def paketRestore = project.tasks[PaketGetPlugin.RESTORE_TASK_NAME]

            [paketInstall, paketUpdate, paketRestore].each { Task task ->
                task.finalizedBy tasks[INSTALL_TASK_NAME]
            }
        }
    }
}

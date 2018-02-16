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
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.get.tasks.PaketUpdate
import wooga.gradle.paket.unity.tasks.PaketUnityInstall

class PaketUnityPlugin implements Plugin<Project> {

    Project project

    static final String GROUP = "PaketUnity"
    static final String EXTENSION_NAME = 'paketUnity'
    static final String INSTALL_TASK_NAME = "paketUnityInstall"

    @Override
    void apply(Project project) {
        this.project = project

        project.pluginManager.apply(PaketBasePlugin.class)

        final extension = project.extensions.create(EXTENSION_NAME, DefaultPaketUnityPluginExtension, project)
        createPaketUnityInstallTasks(project, extension)

        project.tasks.matching({ it.name.startsWith("paketUnity") }).each { task ->
            task.onlyIf {
                extension.getPaketReferencesFiles() && !extension.getPaketReferencesFiles().isEmpty() &&
                        extension.getPaketDependenciesFile() && extension.getPaketDependenciesFile().exists() &&
                        extension.getPaketLockFile() && extension.getPaketLockFile().exists()
            }
        }

        configurePaketDependencyInstallIfPresent()
    }

    private static void createPaketUnityInstallTasks(final Project project, final PaketUnityPluginExtension extension) {

        def lifecycleTask = project.tasks.create(INSTALL_TASK_NAME)
        extension.paketReferencesFiles.each { referenceFile ->
            def task = project.tasks.create(INSTALL_TASK_NAME + referenceFile.parentFile.name, PaketUnityInstall.class)
            task.conventionMapping.map("paketOutputDirectoryName", { extension.getGetPaketOutputDirectoryName() })
            task.lockFile = extension.getPaketLockFile()
            task.referencesFile = referenceFile
            task.projectRoot = referenceFile.parentFile
            task.group = GROUP
            lifecycleTask.dependsOn task
        }
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

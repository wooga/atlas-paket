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

package wooga.gradle.paket.get

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.base.DefaultPaketPluginExtension
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.tasks.PaketInstall
import wooga.gradle.paket.get.tasks.PaketRestore
import wooga.gradle.paket.get.tasks.PaketUpdate

class PaketGetPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String GROUP = "Paket"
    static final String INSTALL_TASK_NAME = "paketInstall"
    static final String RESTORE_TASK_NAME = "paketRestore"
    static final String UPDATE_TASK_NAME = "paketUpdate"

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(PaketBasePlugin.class)

        def paketBootstrap = tasks[PaketBasePlugin.BOOTSTRAP_TASK_NAME]
        def extension = project.extensions.getByType(DefaultPaketPluginExtension)

        def paketInstall = tasks.create(INSTALL_TASK_NAME, PaketInstall.class)
        def paketUpdate = tasks.create(UPDATE_TASK_NAME, PaketUpdate.class)
        def paketRestore = tasks.create(RESTORE_TASK_NAME, PaketRestore.class)

        [paketInstall, paketUpdate, paketRestore].each { Task task ->
            task.paketExtension = extension
            task.group = GROUP
            task.dependsOn paketBootstrap
        }
    }
}

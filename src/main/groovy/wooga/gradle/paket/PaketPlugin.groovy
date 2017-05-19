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

package wooga.gradle.paket

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.base.DefaultPaketPluginExtension
import wooga.gradle.paket.base.tasks.*

class PaketPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String EXTENSION_NAME = 'paket'
    static final String GROUP = "Paket"

    static final String DEPENDENCIES_FILE_NAME = "paket.dependencies"

    static final String BOOTSTRAP_TASK_NAME = "paketBootstrap"
    static final String INSTALL_TASK_NAME = "paketInstall"
    static final String RESTORE_TASK_NAME = "paketRestore"
    static final String UPDATE_TASK_NAME = "paketUpdate"

    static final String PAKET_CONFIGURATION = "nupkg"

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(BasePlugin.class)

        def extension = project.extensions.create(EXTENSION_NAME, DefaultPaketPluginExtension, false)

        //bootstrap
        def paketBootstrap = tasks.create(BOOTSTRAP_TASK_NAME, PaketBootstrap.class)
        paketBootstrap.outputDir = { "$project.projectDir/${extension.paketDirectory}" }
        paketBootstrap.paketBootstrapperFileName = { extension.paketBootstrapperFileName }
        paketBootstrap.bootstrapURL = { extension.paketBootstrapperDownloadURL }
        paketBootstrap.paketExtension = extension
        paketBootstrap.paketVersion = { extension.version }

        paketBootstrap.paketBootstrapper = {"${paketBootstrap.outputDir.path}/${extension.paketBootstrapperFileName}"}
        paketBootstrap.paketFile = { "${paketBootstrap.outputDir.path}/${extension.paketExecuteableName}" }

        //init
        def init = tasks.create(name: 'paketInit', type: PaketInit, group: GROUP)
        init.finalizedBy paketBootstrap

        //install
        def paketInstall = tasks.create(INSTALL_TASK_NAME, PaketInstall.class)
        def paketUpdate = tasks.create(UPDATE_TASK_NAME, PaketUpdate.class)
        def paketRestore = tasks.create(RESTORE_TASK_NAME, PaketRestore.class)

        [paketInstall, paketUpdate, paketRestore].each { Task task ->
            task.paketExtension = extension
            task.group = GROUP
            task.dependsOn paketBootstrap
        }

        def configurations = project.configurations
        configurations.create(PAKET_CONFIGURATION) {
            description = "paket nupkg archive"
            transitive = false
        }
    }
}

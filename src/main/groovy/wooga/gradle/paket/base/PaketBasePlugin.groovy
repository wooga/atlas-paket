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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import wooga.gradle.paket.base.tasks.PaketBootstrap
import wooga.gradle.paket.get.tasks.PaketInit

class PaketBasePlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String EXTENSION_NAME = 'paket'
    static final String DEPENDENCIES_FILE_NAME = "paket.dependencies"
    static final String BOOTSTRAP_TASK_NAME = "paketBootstrap"
    static final String INIT_TASK_NAME = "paketInit"
    static final String PAKET_CONFIGURATION = "nupkg"

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        def extension = project.extensions.create(EXTENSION_NAME, DefaultPaketPluginExtension)

        //bootstrap
        PaketBootstrap paketBootstrap = tasks.create(name:BOOTSTRAP_TASK_NAME, type:PaketBootstrap)
        paketBootstrap.outputDir = { "$project.projectDir/${extension.paketDirectory}" }
        paketBootstrap.paketBootstrapperFileName = { extension.paketBootstrapperFileName }
        paketBootstrap.bootstrapURL = { extension.paketBootstrapperDownloadURL }
        paketBootstrap.paketExtension = extension
        paketBootstrap.paketVersion = { extension.version }

        paketBootstrap.paketBootstrapper = {"${paketBootstrap.outputDir.path}/${extension.paketBootstrapperFileName}"}
        paketBootstrap.paketFile = { "${paketBootstrap.outputDir.path}/${extension.paketExecuteableName}" }

        //init
        def init = tasks.create(name: INIT_TASK_NAME, type: PaketInit, group: TaskConfiguration.GROUP)
        init.finalizedBy paketBootstrap

        def configurations = project.configurations
        configurations.create(PAKET_CONFIGURATION) {
            description = "paket nupkg archive"
            transitive = false
        }
    }
}

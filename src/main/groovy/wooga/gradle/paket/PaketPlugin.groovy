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
import wooga.gradle.paket.tasks.*

class PaketPlugin implements Plugin<Project> {

    Project project

    static final String WOOGA_PAKET_EXTENSION_NAME = 'paket'
    static final String WOOGA_PAKET_UNITY_EXTENSION_NAME = 'paketUnity'

    static final String PAKET_GROUP = "Paket"

    static final String DEPENDENCIES_FILE_NAME = "paket.dependencies"

    static final String RESTORE_TASK_NAME = "paketRestore"
    static final String UPDATE_TASK_NAME = "paketUpdate"

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: 'base'

        //bootstrap
        def paketExtension = project.extensions.create(WOOGA_PAKET_EXTENSION_NAME, PaketPluginExtension, false)
        def paketUnityExtension = project.extensions.create(WOOGA_PAKET_UNITY_EXTENSION_NAME, PaketPluginExtension, true)

        def paketBootstrap = createPaketBootstrapTasks(WOOGA_PAKET_EXTENSION_NAME, paketExtension)
        def paketUnityBootstrap = createPaketBootstrapTasks(WOOGA_PAKET_UNITY_EXTENSION_NAME, paketUnityExtension)

        //init task
        project.tasks.create(name: 'paketInit', type: PaketInit, group: PAKET_GROUP)

        //install
        def paketInstall = createPaketInstallTask(WOOGA_PAKET_EXTENSION_NAME, paketExtension)
        paketInstall.dependsOn paketBootstrap

        def paketUpdate = project.tasks.create(name: UPDATE_TASK_NAME, dependsOn: paketBootstrap, group: PAKET_GROUP, type: PaketUpdate)
        def paketRestore = project.tasks.create(name: RESTORE_TASK_NAME, dependsOn: paketBootstrap, group: PAKET_GROUP, type: PaketRestore)

        def paketUnityInstall = createPaketInstallTask(WOOGA_PAKET_UNITY_EXTENSION_NAME, paketUnityExtension)
        paketUnityInstall.dependsOn paketUnityBootstrap

        [paketInstall, paketUpdate, paketRestore].each { Task task ->
            task.finalizedBy paketUnityInstall
        }

        project.tasks.matching({it.name.startsWith("paketUnity")}).each { PaketTask task ->
            task.paketDependencies = {
                project.fileTree(project.projectDir).include("**/paket.unity3d.references")
            }

            task.paketExtension = paketUnityExtension

            task.onlyIf {
                project.file("$project.projectDir/${PaketPlugin.DEPENDENCIES_FILE_NAME}").exists()
            }
        }
    }

    private PaketTask createPaketInstallTask(String taskPrefix, PaketPluginExtension extension) {
        def installName = taskPrefix + 'Install'
        def install = project.tasks.create(name: installName, type: PaketInstall) {
            paketExtension = extension
            group PAKET_GROUP
        }

        return install
    }

    private PaketTask createPaketBootstrapTasks(String taskPrefix, PaketPluginExtension extension) {
        def bootstrapDownloadName = taskPrefix + 'BootstrapDownload'
        def bootstrapDownload = project.tasks.create(name: bootstrapDownloadName, type: PaketBootstrapDownload) {
            outputDir = { "$project.projectDir/${extension.paketDirectory}" }
            paketBootstrapperFileName = {
                extension.paketBootstrapperFileName
            }
            bootstrapURL = {
                extension.paketBootstrapperDownloadURL
            }

            paketExtension = extension
        }

        def bootstrapName = taskPrefix + 'Bootstrap'
        def bootstrap = project.tasks.create(name: bootstrapName, type: PaketBootstrap) {
            dependsOn bootstrapDownload
            paketExtension = extension

            paketVersion = { extension.version }
            paketBootstrapper = {
                "${bootstrapDownload.outputDir.path}/${extension.paketBootstrapperFileName}"
            }
            paketFile = {
                "${bootstrapDownload.outputDir.path}/${extension.paketExecuteableName}"
            }
        }

        return bootstrap
    }
}

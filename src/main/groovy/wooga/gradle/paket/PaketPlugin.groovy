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
import wooga.gradle.paket.tasks.*

class PaketPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String WOOGA_PAKET_EXTENSION_NAME = 'paket'
    static final String WOOGA_PAKET_UNITY_EXTENSION_NAME = 'paketUnity'

    static final String PAKET_GROUP = "Paket"

    static final String DEPENDENCIES_FILE_NAME = "paket.dependencies"

    static final String RESTORE_TASK_NAME = "paketRestore"
    static final String UPDATE_TASK_NAME = "paketUpdate"
    static final String PAKET_CONFIGURATION = "nupkg"

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(BasePlugin.class)

        //bootstrap
        def paketExtension = project.extensions.create(WOOGA_PAKET_EXTENSION_NAME, PaketPluginExtension, false)
        def paketUnityExtension = project.extensions.create(WOOGA_PAKET_UNITY_EXTENSION_NAME, PaketPluginExtension, true)

        def paketBootstrap = createPaketBootstrapTasks(WOOGA_PAKET_EXTENSION_NAME, paketExtension)
        def paketUnityBootstrap = createPaketBootstrapTasks(WOOGA_PAKET_UNITY_EXTENSION_NAME, paketUnityExtension)
        paketUnityBootstrap.unity = true

        //init task
        tasks.create(name: 'paketInit', type: PaketInit, group: PAKET_GROUP)

        //install
        def paketInstall = createPaketInstallTask(WOOGA_PAKET_EXTENSION_NAME, paketExtension)
        paketInstall.dependsOn paketBootstrap

        def paketUpdate = tasks.create(name: UPDATE_TASK_NAME, dependsOn: paketBootstrap, group: PAKET_GROUP, type: PaketUpdate)
        def paketRestore = tasks.create(name: RESTORE_TASK_NAME, dependsOn: paketBootstrap, group: PAKET_GROUP, type: PaketRestore)

        def paketUnityInstall = createPaketInstallTask(WOOGA_PAKET_UNITY_EXTENSION_NAME, paketUnityExtension)
        paketUnityInstall.dependsOn paketUnityBootstrap

        [paketInstall, paketUpdate, paketRestore].each { Task task ->
            task.finalizedBy paketUnityInstall
        }

        tasks.matching({it.name.startsWith("paketUnity")}).each { PaketTask task ->
            task.paketDependencies = {
                project.fileTree(project.projectDir).include("**/paket.unity3d.references")
            }

            task.paketExtension = paketUnityExtension

            task.onlyIf {
                project.file("$project.projectDir/${PaketPlugin.DEPENDENCIES_FILE_NAME}").exists()
            }
        }

        def configurations = project.configurations
        configurations.create(PAKET_CONFIGURATION) {
            description = "paket nupkg archive"
            transitive = false
        }

        project.afterEvaluate {
            def templateFiles = project.fileTree(project.projectDir)
            templateFiles.include "**/paket.template"
            templateFiles.each { File file ->
                def templateReader = new PaketTemplateReader(file)
                def packageID = templateReader.getPackageId()
                def packageName = packageID.replaceAll(/\./, '')
                PaketPack packTask = tasks.create(name: 'paketPack-' + packageName, group: BasePlugin.BUILD_GROUP, type: PaketPack)
                packTask.templateFile = file
                packTask.outputDir = { "$project.buildDir/outputs" }
                packTask.outputs.file { "$packTask.outputDir/${packageID}.${project.version}.nupkg" }
                packTask.version = { project.version }
                packTask.description = "Pack package ${templateReader.getPackageId()}"
                packTask.dependsOn paketInstall

                tasks[BasePlugin.ASSEMBLE_TASK_NAME].dependsOn packTask

                project.artifacts.add(PAKET_CONFIGURATION, [file: project.file("$project.buildDir/outputs/${packageID}.${project.version}.nupkg"), name: packageID, builtBy: packTask])
            }
        }
    }

    private class PaketTemplateReader {

        private def content

        PaketTemplateReader(File templateFile) {
            content = [:]
            templateFile.eachLine { line ->
                def matcher
                if ((matcher = line =~ /^(\w+)( |\n[ ]{4})(((\n[ ]{4})?.*)+)/)) {
                    content[matcher[0][1]] = matcher[0][3]
                }
            }
        }

        String getPackageId() {
            content['id']
        }
    }

    private PaketTask createPaketInstallTask(String taskPrefix, PaketPluginExtension extension) {
        def installName = taskPrefix + 'Install'
        def install = tasks.create(name: installName, type: PaketInstall) {
            paketExtension = extension
            group PAKET_GROUP
        }

        return install
    }

    private PaketBootstrap createPaketBootstrapTasks(String taskPrefix, PaketPluginExtension extension) {
        def bootstrapDownloadName = taskPrefix + 'BootstrapDownload'
        def bootstrapDownload = tasks.create(name: bootstrapDownloadName, type: PaketBootstrapDownload) {
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
        def bootstrap = tasks.create(name: bootstrapName, type: PaketBootstrap) {
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

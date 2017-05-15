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

package net.wooga.gradle

import net.wooga.gradle.tasks.PaketBootstrap
import net.wooga.gradle.tasks.PaketBootstrapDownload
import net.wooga.gradle.tasks.PaketInit
import net.wooga.gradle.tasks.PaketInstall
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class PaketPlugin implements Plugin<Project> {

    Project project

    static final String WOOGA_PAKET_EXTENSION_NAME = 'paket'
    static final String WOOGA_PAKET_UNITY_EXTENSION_NAME = 'paketUnity'

    static final String GROUP = "Paket"

    @Override
    void apply(Project project) {
        this.project = project

        def paketExtension = project.extensions.create(WOOGA_PAKET_EXTENSION_NAME, PaketPluginExtension, false)
        def paketUnityExtension = project.extensions.create(WOOGA_PAKET_UNITY_EXTENSION_NAME, PaketPluginExtension, true)

        def paketBootstrap = createPaketBootstrapTasks("paket", paketExtension)
        def paketUnityBootstrap = createPaketBootstrapTasks("paketUnity",paketUnityExtension)

        def init = project.tasks.create(name:'init', type:PaketInit, dependsOn: paketBootstrap)
        init.paketExtension = paketExtension

        init.onlyIf { !project.file("${project.projectDir}/paket.dependencies").exists()}

        def paketInstall = createPaketInstallTask("paket", paketExtension)
        paketInstall.dependsOn init

        def paketUnityInstall = createPaketInstallTask("paketUnity", paketUnityExtension)
        paketUnityInstall.dependsOn paketUnityBootstrap
        paketUnityInstall.mustRunAfter paketInstall

        project.tasks.create(name: 'install', dependsOn: [paketInstall, paketUnityInstall], group: GROUP)
    }

    private Task createPaketInstallTask(String taskPrefix, PaketPluginExtension extension) {
        def installName = taskPrefix + 'Install'
        def install = project.tasks.create(name: installName, type: PaketInstall) {
            paketExtension = extension
        }

        return install
    }

    private Task createPaketBootstrapTasks(String taskPrefix, PaketPluginExtension extension) {
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

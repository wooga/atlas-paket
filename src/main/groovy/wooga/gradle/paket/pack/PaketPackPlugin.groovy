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

package wooga.gradle.paket.pack

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.PaketPlugin
import wooga.gradle.paket.base.tasks.PaketInstall
import wooga.gradle.paket.pack.tasks.PaketPack

class PaketPackPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static String PAKET_TEMPLATE_PATTERN = "**/paket.template"
    static String TASK_PACK_PREFIX = "paketPack-"

    @Override
    void apply(Project project) {

        this.project = project
        this.tasks = project.tasks

        project.afterEvaluate {
            def templateFiles = project.fileTree(project.projectDir)
            templateFiles.include PAKET_TEMPLATE_PATTERN
            templateFiles.each { File file ->
                def templateReader = new PaketTemplateReader(file)
                def packageID = templateReader.getPackageId()
                def packageName = packageID.replaceAll(/\./, '')
                def packTask = tasks.create(TASK_PACK_PREFIX + packageName, PaketPack.class)
                packTask.group = BasePlugin.BUILD_GROUP
                packTask.templateFile = file
                packTask.outputDir = { "$project.buildDir/outputs" }
                packTask.outputs.file { "$packTask.outputDir/${packageID}.${project.version}.nupkg" }
                packTask.version = { project.version }
                packTask.description = "Pack package ${templateReader.getPackageId()}"

                packTask.dependsOn tasks[PaketPlugin.INSTALL_TASK_NAME]

                tasks[BasePlugin.ASSEMBLE_TASK_NAME].dependsOn packTask

                project.artifacts.add(PaketPlugin.PAKET_CONFIGURATION, [file: project.file("$project.buildDir/outputs/${packageID}.${project.version}.nupkg"), name: packageID, builtBy: packTask])
            }

            configurePaketInstallIfPresent()
        }
    }

    void configurePaketInstallIfPresent() {
        project.plugins.withType(PaketPlugin) {
            project.tasks.withType(PaketPack) { task ->
                task.dependsOn project.tasks[PaketPlugin.INSTALL_TASK_NAME]
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
}

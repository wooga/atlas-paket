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

package wooga.gradle.paket.pack

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.base.DefaultPaketPluginExtension
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.pack.tasks.PaketPack

class PaketPackPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(PaketPackPlugin)

    Project project
    TaskContainer tasks

    static String PAKET_TEMPLATE_PATTERN = "**/paket.template"
    static String TASK_PACK_PREFIX = "paketPack-"

    @Override
    void apply(Project project) {

        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(BasePlugin.class)
        project.pluginManager.apply(PaketBasePlugin.class)

        def paketBootstrap = tasks[PaketBasePlugin.BOOTSTRAP_TASK_NAME]
        def extension = project.extensions.getByType(DefaultPaketPluginExtension)

        def templateFiles = project.fileTree(project.projectDir)
        templateFiles.include PAKET_TEMPLATE_PATTERN
        templateFiles.sort()
        templateFiles = templateFiles.sort(true, new Comparator<File>() {
            @Override
            int compare(File o1, File o2) {
                String sep = File.separator
                if(o1.path.count(sep) > o2.path.count(sep)) {
                    return 1
                }
                else if(o1.path.count(sep) < o2.path.count(sep)) {
                    return -1
                }
                else
                {
                    return 0
                }
            }
        })

        templateFiles.each { File file ->
            def templateReader = new PaketTemplateReader(file)
            def packageID = templateReader.getPackageId()
            def packageName = packageID.replaceAll(/\./, '')
            def taskName = TASK_PACK_PREFIX + packageName

            def packTask = tasks.findByName(taskName)
            if (packTask && PaketPack.isInstance(packTask)) {
                File templateFileInUse = ((PaketPack) packTask).templateFile
                logger.warn("Multiple paket.template files with id ${packageID}.")
                logger.warn("Template file with same id already in use $templateFileInUse.path")
                logger.warn("Skip template file: $file.path")
                return
            }

            packTask = tasks.create(taskName, PaketPack.class)

            packTask.group = BasePlugin.BUILD_GROUP
            packTask.templateFile = file
            packTask.outputDir = project.file("${project.buildDir}/outputs")
            packTask.version = project.version
            packTask.description = "Pack package ${templateReader.getPackageId()}"
            packTask.paketExtension = extension
            packTask.dependsOn paketBootstrap

            tasks[BasePlugin.ASSEMBLE_TASK_NAME].dependsOn packTask

            project.artifacts.add(PaketBasePlugin.PAKET_CONFIGURATION, [file: packTask.outputFile, name: packageID, builtBy: packTask])
        }

        configurePaketInstallIfPresent()
    }

    void configurePaketInstallIfPresent() {
        project.plugins.withType(PaketGetPlugin) {
            project.tasks.withType(PaketPack) { task ->
                task.dependsOn project.tasks[PaketGetPlugin.INSTALL_TASK_NAME]
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

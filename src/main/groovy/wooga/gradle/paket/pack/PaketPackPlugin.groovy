/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.pack

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.base.PaketPluginExtension
import wooga.gradle.paket.base.utils.internal.PaketTemplate
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.pack.internal.DefaultPaketPackPluginExtension
import wooga.gradle.paket.pack.internal.PaketPublishingArtifact
import wooga.gradle.paket.pack.tasks.PaketPack

/**
 * A {@link Plugin} which adds tasks to pack nuget files with {@code paket}.
 * <p>
 * The plugin adds a pack task for each {@code paket.template} file found recursively in the project.
 * All {@code pack} tasks will be added as a dependency to {@code assemble}.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins {
 *         id 'net.wooga.paket-get' version '0.10.1'
 *     }
 * }
 * </pre>
 */
class PaketPackPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(PaketPackPlugin)

    Project project
    TaskContainer tasks

    static String PAKET_TEMPLATE_PATTERN = "**/paket.template"
    static String TASK_PACK_PREFIX = "paketPack-"
    static final String EXTENSION_NAME = 'paketPack'


    @Override
    void apply(Project project) {

        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(BasePlugin.class)
        project.pluginManager.apply(PaketBasePlugin.class)
        final PaketPluginExtension baseExtension = project.extensions.getByName(PaketBasePlugin.EXTENSION_NAME) as PaketPluginExtension
        final extension = project.extensions.create(EXTENSION_NAME, DefaultPaketPackPluginExtension, project, baseExtension.dependencyHandler)

        final configuration = project.configurations.getByName(PaketBasePlugin.PAKET_CONFIGURATION)
        def templateFiles = project.fileTree(project.projectDir)
        templateFiles.include PAKET_TEMPLATE_PATTERN
        templateFiles = templateFiles.sort()
        templateFiles = templateFiles.sort(true) { o1, o2 ->
            String sep = File.separator
            if (o1.path.count(sep) > o2.path.count(sep)) {
                return 1
            } else if (o1.path.count(sep) < o2.path.count(sep)) {
                return -1
            } else {
                return 0
            }
        }

        templateFiles.each { File file ->
            def templateReader = new PaketTemplate(file)
            def packageID = templateReader.getPackageId()
            def packageName = packageID.replaceAll(/\./, '')
            def taskName = TASK_PACK_PREFIX + packageName

            def packTaskProvider = findTask(tasks, taskName, PaketPack).orElse(null)

            if (packTaskProvider) {
                PaketPack packTask = packTaskProvider.get() as PaketPack; //this will realize the task
                File templateFileInUse = packTask.templateFile
                logger.warn("Multiple paket.template files with id ${packageID}.")
                logger.warn("Template file with same id already in use $templateFileInUse.path")
                logger.warn("Skip template file: $file.path")
            } else {
                packTaskProvider = tasks.register(taskName, PaketPack.class)
                packTaskProvider.configure { packTask ->
                    packTask.group = BasePlugin.BUILD_GROUP
                    packTask.description = "Pack package ${packageID}"
                    packTask.templateFile = file

                }
                tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure {t -> t.dependsOn(packTaskProvider) }
                PublishArtifact artifact = PaketPublishingArtifact.fromTask(packTaskProvider, tasks as TaskResolver, packageID)
                configuration.getArtifacts().add(artifact)
            }
        }
        configurePaketInstallIfPresent()
        configurePaketPackDefaults(extension)
    }

    private void configurePaketPackDefaults(PaketPluginExtension extension) {
        tasks.withType(PaketPack).configureEach {task ->
            def paketTemplate = new PaketTemplate(task.templateFile)

            ConventionMapping taskConventionMapping = task.getConventionMapping()

            // already set on creation, right?
            //taskConventionMapping.map("templateFile", { extension.getBaseUrl() })
            taskConventionMapping.map("outputDir", { project.file("${project.buildDir}/outputs") })
            taskConventionMapping.map("version", {
                paketTemplate.version ?: extension.getVersion()
            })
        }
    }

    void configurePaketInstallIfPresent() {
        project.plugins.withType(PaketGetPlugin) {
            project.tasks.withType(PaketPack).configureEach { task ->
                task.dependsOn(project.tasks.named(PaketGetPlugin.INSTALL_TASK_NAME))
            }
        }
    }

    static Optional<TaskProvider> findTask(TaskContainer tasks, String taskName, Class<? extends Task> clazz) {
        try {
            return Optional.of(tasks.named(taskName, clazz))
        } catch(UnknownTaskException _) {
            return Optional.empty()
        }

    }
}

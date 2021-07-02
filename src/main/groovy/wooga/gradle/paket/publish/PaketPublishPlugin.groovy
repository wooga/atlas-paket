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

package wooga.gradle.paket.publish

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.publish.internal.DefaultPaketPushPluginExtension
import wooga.gradle.paket.publish.repository.internal.DefaultNugetRepositoryHandlerConvention
import wooga.gradle.paket.publish.repository.internal.NugetRepository
import wooga.gradle.paket.publish.tasks.PaketPush
import wooga.gradle.paket.publish.tasks.internal.PaketCopy

/**
 * A {@link Plugin} which adds tasks to publish nuget files with {@code paket}.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins {
 *         id 'net.wooga.paket-publish' version '0.10.1'
 *     }
 * }
 * </pre>
 */
class PaketPublishPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(PublishingPlugin.class)
        project.pluginManager.apply(PaketBasePlugin.class)

        def pushExtension = project.extensions.create(DefaultPaketPushPluginExtension.NAME, DefaultPaketPushPluginExtension, project)
        def publishLifecycleTask = tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)

        project.getExtensions().configure(PublishingExtension.class, new Action<PublishingExtension>() {
            void execute(PublishingExtension e) {
                RepositoryHandler repositories = e.repositories
                DefaultRepositoryHandler handler = (DefaultRepositoryHandler) repositories

                DefaultNugetRepositoryHandlerConvention repositoryConvention = new DefaultNugetRepositoryHandlerConvention(handler)
                new DslObject(repositories).getConvention().getPlugins().put("net.wooga.paket-publish", repositoryConvention)
            }
        })

        project.afterEvaluate {
            PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)

            Configuration nupkg = project.configurations.getByName(PaketBasePlugin.PAKET_CONFIGURATION)
            String publishRepositoryName = pushExtension.publishRepositoryName

            try {
                publishingExtension.repositories.withType(NugetRepository).getByName(publishRepositoryName) { NugetRepository repository ->
                    nupkg.allArtifacts.each { artifact ->
                        def packageName = artifact.name.replaceAll(/\./, '')
                        def publishTaskName = "publish-$packageName"
                        createPublishTask(publishTaskName, artifact, publishLifecycleTask, repository)
                    }
                }
            } catch (Exception e) {
                project.logger.warn(e.message)
            }

            PublishArtifactSet artifacts = nupkg.allArtifacts

            publishingExtension.repositories.withType(NugetRepository) { repository ->
                createPublishTasks(tasks, artifacts, repository)
            }
        }

        createPublishLocalTask(tasks)
    }

    static void createPublishLocalTask(TaskContainer tasks) {
        def paketCopylifecycle = tasks.create(name: "publishLocal", group: PublishingPlugin.PUBLISH_TASK_GROUP, description: "publish all nupkg to all local paths")
        paketCopylifecycle.dependsOn tasks.withType(PaketCopy)
    }

    void createPublishTasks(TaskContainer tasks, PublishArtifactSet artifacts, NugetRepository repository) {

        def baseTaskName = "publish" + repository.name.capitalize()
        def repoLifecycle = tasks.register(baseTaskName)
        repoLifecycle.configure { t ->
          t.group = PublishingPlugin.PUBLISH_TASK_GROUP
            t.description = "Publishes all nupkg artifacts to ${repository.destination}"
        }

        artifacts.each { artifact ->
            def packageName = artifact.name.replaceAll(/\./, '')
            def publishTaskName = "$baseTaskName-$packageName"
            createPublishTask(publishTaskName, artifact, repoLifecycle, repository)
        }
    }

    private TaskProvider<? extends Task> createPublishTask(String publishTaskName, PublishArtifact artifact, TaskProvider lifecycle, NugetRepository repository) {
        TaskProvider taskProvider = repository.url != null ? createPaketPublishTask(publishTaskName, repository, artifact) :
                createPaketCopyTask(publishTaskName, repository, artifact)
        lifecycle.configure { Task t -> t.dependsOn(taskProvider) }
        taskProvider.configure { task ->
            task.dependsOn(artifact)
        }
        return taskProvider
    }

    private TaskProvider<PaketCopy> createPaketCopyTask(String publishTaskName, NugetRepository repository, PublishArtifact artifact) {
        TaskProvider<PaketCopy> taskProvider = tasks.register(publishTaskName, PaketCopy)
        taskProvider.configure {task ->
            task.group = PublishingPlugin.PUBLISH_TASK_GROUP
            task.description = "Copies ${artifact.file.name} to ${repository.path}"
            task.from(artifact.file)
            task.into(new File(repository.path))
        }
        return taskProvider
    }

    private TaskProvider<PaketPush> createPaketPublishTask(String publishTaskName, NugetRepository repository, PublishArtifact artifact) {
        TaskProvider<PaketPush> taskProvider = tasks.register(publishTaskName, PaketPush)
        taskProvider.configure { task ->
            task.group = PublishingPlugin.PUBLISH_TASK_GROUP
            task.url = repository.url
            task.apiKey = repository.apiKey
            task.endpoint = repository.endpoint
            task.inputFile = { artifact.file }
            task.description = "Publishes ${artifact.name} to ${repository.url}"
        }
        return taskProvider
    }
}

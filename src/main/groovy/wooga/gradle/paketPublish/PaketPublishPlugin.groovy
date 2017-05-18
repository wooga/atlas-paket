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

package wooga.gradle.paketPublish

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.PaketPlugin
import wooga.gradle.paketPublish.tasks.PaketPush

class PaketPublishPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(PublishingPlugin.class)
        project.pluginManager.apply(PaketPlugin.class)

        def extension = project.extensions.create(DefaultPaketPushPluginExtension.NAME, DefaultPaketPushPluginExtension, project)
        def publishLifecycleTask = tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME]

        project.getExtensions().configure(PublishingExtension.class, new Action<PublishingExtension>() {
            public void execute(PublishingExtension e) {
                RepositoryHandler repositories = e.repositories
                DefaultRepositoryHandler handler = (DefaultRepositoryHandler) repositories

                DefaultNugetRepositoryHandlerConvention repositoryConvention = new DefaultNugetRepositoryHandlerConvention(handler)
                new DslObject(repositories).getConvention().getPlugins().put("net.wooga.paket-publish", repositoryConvention)
            }
        })

        project.afterEvaluate {
            PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)

            Configuration nupkg = project.configurations.getByName(PaketPlugin.PAKET_CONFIGURATION)

            String publishRepositoryName = extension.publishRepositoryName

            try {
                publishingExtension.repositories.withType(NugetRepository).getByName(publishRepositoryName) { NugetRepository repository ->
                    nupkg.allArtifacts.each { artifact ->
                        def packageName = artifact.name.replaceAll(/\./, '')
                        def publishTaskName = "publish-$packageName"
                        PaketPush pushTask = (PaketPush) tasks.create(name: publishTaskName, group: PublishingPlugin.PUBLISH_TASK_GROUP, type: PaketPush)

                        pushTask.url = { repository.url }
                        pushTask.apiKey = { repository.apiKey }
                        pushTask.inputFile = artifact.file
                        pushTask.description = "Publishes ${artifact.file.name} to ${repository.url}"
                        pushTask.dependsOn artifact
                        publishLifecycleTask.dependsOn pushTask
                    }
                }
            }
            catch (Exception e) {

            }
            
            PublishArtifactSet artifacts = nupkg.allArtifacts

            publishingExtension.repositories.withType(NugetRepository) {
                createPublishTasks(tasks, artifacts, it)
            }
        }
    }

    void createPublishTasks(TaskContainer tasks, PublishArtifactSet artifacts, NugetRepository repository) {
        String baseTaskName = "publish" + repository.name.capitalize()
        Task repoLifecycle = tasks.create(name: baseTaskName, group: PublishingPlugin.PUBLISH_TASK_GROUP)
        repoLifecycle.description = "Publishes all nupkg artifacts to ${repository.url}"

        artifacts.each { artifact ->
            def packageName = artifact.name.replaceAll(/\./, '')
            def publishTaskName = "$baseTaskName-$packageName"
            PaketPush pushTask = (PaketPush) tasks.create(name: publishTaskName, group: PublishingPlugin.PUBLISH_TASK_GROUP, type: PaketPush)
            pushTask.url = repository.url
            pushTask.apiKey = repository.apiKey
            pushTask.inputFile = artifact.file
            pushTask.description = "Publishes ${artifact.file.name} to ${repository.url}"
            pushTask.dependsOn artifact

            repoLifecycle.dependsOn pushTask
        }
    }
}

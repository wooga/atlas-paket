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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.paket.PaketPlugin
import wooga.gradle.paketPublish.tasks.PaketPush

class PaketPublishPlugin implements Plugin<Project> {

    Project project
    TaskContainer tasks

    static final String WOOGA_PAKET_PUBLISH_EXTENSION_NAME = 'paketPublish'

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(PublishingPlugin.class)
        project.pluginManager.apply(PaketPlugin.class)

        def extension = project.extensions.create(WOOGA_PAKET_PUBLISH_EXTENSION_NAME, PaketPushPluginExtension, project)

        def publishLifecycleTask = tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME]

        Configuration nupkg = project.configurations.getByName(PaketPlugin.PAKET_CONFIGURATION)
        nupkg.allArtifacts.each { artifact ->
            def packageName = artifact.name.replaceAll(/\./,'')
            def publishTaskName = "paketPush-$packageName"
            PaketPush pushTask = tasks.create(name: publishTaskName, group: PublishingPlugin.PUBLISH_TASK_GROUP, type: PaketPush)
            pushTask.url = { extension.getPublishURL() }
            pushTask.apiKey = { extension.getApiKey() }
            pushTask.inputFile = artifact.file
            pushTask.dependsOn artifact
            pushTask.doLast {
                println "push ME"
            }
            publishLifecycleTask.dependsOn pushTask
        }
    }
}

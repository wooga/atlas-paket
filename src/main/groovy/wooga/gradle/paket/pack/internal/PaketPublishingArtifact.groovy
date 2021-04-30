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

package wooga.gradle.paket.pack.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.paket.pack.tasks.PaketPack

class PaketPublishingArtifact implements PublishArtifact {

    @Override
    String getName() {
        return packageId
    }

    @Override
    String getExtension() {
        return "nupkg"
    }

    @Override
    String getType() {
        return "zip"
    }

    @Override
    String getClassifier() {
        return null
    }

    @Override
    File getFile() {
        return this.task.get().getOutputFile()
    }

    @Override
    Date getDate() {
        return null
    }

    def taskDependency

    @Override
    TaskDependency getBuildDependencies() {
        taskDependency
    }

    private TaskProvider<PaketPack> task
    private final String packageId

    static PublishArtifact fromTask(TaskProvider<PaketPack> task, TaskResolver tasks, String packId) {
        new PaketPublishingArtifact(task, tasks, packId)
    }

    PaketPublishingArtifact(TaskProvider<PaketPack> task, TaskResolver tasks, String packId) {
        this.task = task
        this.packageId = packId
        taskDependency = new DefaultTaskDependency(tasks)
        taskDependency.add(task)
    }
}

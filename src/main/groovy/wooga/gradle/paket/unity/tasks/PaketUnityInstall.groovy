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

package wooga.gradle.paket.unity.tasks

import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences
import wooga.gradle.paket.unity.PaketUnityPlugin

class PaketUnityInstall extends ConventionTask {

    @InputFile
    File referencesFile

    @Input
    String paketOutputDir

    PaketUnityInstall() {
        description = 'Copy paket dependencies into unity projects'
        group = PaketUnityPlugin.GROUP
    }

    @TaskAction
    protected performCopy() {

        def projectCopySpec = project.copySpec()
        def references = new PaketUnityReferences(referencesFile)
        references.nugets.each { nuget ->
            projectCopySpec.from("packages/${nuget}/content", new Action<CopySpec>() {

                @Override
                void execute(CopySpec spec) {
                    spec.into("/Assets/${paketOutputDir}/${nuget}")
                    spec.exclude("**/Meta")
                }
            })
        }

        project.sync(new Action<CopySpec>() {
            @Override
            void execute(CopySpec spec) {
                spec.with(projectCopySpec)
                spec.into("${referencesFile.parent}")
            }
        })
    }
}

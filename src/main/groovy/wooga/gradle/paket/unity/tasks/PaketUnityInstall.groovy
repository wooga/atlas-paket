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

package wooga.gradle.paket.unity.tasks

import wooga.gradle.paket.unity.PaketUnityPlugin
import wooga.gradle.paket.base.tasks.AbstractPaketTask

class PaketUnityInstall extends AbstractPaketTask {

    static String COMMAND = "install"

    def paketDependencies = {
        project.fileTree(project.projectDir).include("**/paket.unity3d.references")
    }

    PaketUnityInstall() {
        super(PaketUnityInstall.class)
        description = 'Download the dependencies specified by the paket.dependencies or paket.lock file into the packages/ directory and update projects.'
        group = PaketUnityPlugin.GROUP
        paketCommand = COMMAND
    }
}

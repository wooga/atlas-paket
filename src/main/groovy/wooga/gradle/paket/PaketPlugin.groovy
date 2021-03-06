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

package wooga.gradle.paket

import org.gradle.api.Plugin
import org.gradle.api.Project
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.pack.PaketPackPlugin
import wooga.gradle.paket.publish.PaketPublishPlugin

/**
 * A {@link Plugin} which adds support to fetch, pack and publish {@code nupgk} packages with paket.
 * <p>
 * <pre>
 * {@code
 *     plugins {
 *         id 'net.wooga.paket' version '0.10.1'
 *     }
 * }
 * </pre>
 */
class PaketPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(PaketBasePlugin.class)
        project.pluginManager.apply(PaketGetPlugin.class)
        project.pluginManager.apply(PaketPackPlugin.class)
        project.pluginManager.apply(PaketPublishPlugin.class)
    }
}

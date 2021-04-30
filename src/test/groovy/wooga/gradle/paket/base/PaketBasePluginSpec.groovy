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

package wooga.gradle.paket.base

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.base.tasks.PaketBootstrap

class PaketBasePluginActivationSpec extends PluginProjectSpec {
    @Override
    String getPluginName() { return 'net.wooga.paket-base' }
}

class PaketBasePluginSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.paket-base'

    def 'Creates the [paket] extension'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !hasTask(project, PaketBasePlugin.EXTENSION_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(PaketBasePlugin.EXTENSION_NAME)
        extension instanceof DefaultPaketPluginExtension
    }

    def 'Creates the paket bootstrap task'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !hasTask(project, PaketBasePlugin.BOOTSTRAP_TASK_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(PaketBasePlugin.BOOTSTRAP_TASK_NAME)
        task instanceof PaketBootstrap
    }

    def 'Creates the [nupkg] configuration'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !hasTask(project, PaketBasePlugin.PAKET_CONFIGURATION)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.configurations.findByName(PaketBasePlugin.PAKET_CONFIGURATION)
    }

    def 'Configures the [nupkg] configuration'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !hasTask(project, PaketBasePlugin.PAKET_CONFIGURATION)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def config = project.configurations.findByName(PaketBasePlugin.PAKET_CONFIGURATION)
        !config.transitive
    }

    def hasTask(Project project, String taskName) {
        try {
            project.tasks.named(taskName)
            return true
        } catch(UnknownTaskException _) {
            return false
        }
    }
}

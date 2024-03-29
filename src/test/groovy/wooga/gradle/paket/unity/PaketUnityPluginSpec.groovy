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

package wooga.gradle.paket.unity

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec
import spock.lang.Unroll
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension

class PaketUnityPluginActivationSpec extends PluginProjectSpec {
    @Override
    String getPluginName() { return 'net.wooga.paket-unity' }
}

class PaketUnityPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.paket-unity'

    public static final String GET_PLUGIN_NAME = 'net.wooga.paket-get'

    def 'Creates the [paketUnity] extension'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(PaketUnityPlugin.EXTENSION_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(PaketUnityPlugin.EXTENSION_NAME)
        extension instanceof DefaultPaketUnityPluginExtension
    }

    @Unroll
    def 'adds install task to [paket get] task #taskName'(String taskName) {
        given: "project with [paket get] applied"
        project.plugins.apply(GET_PLUGIN_NAME)

        and: "project with [paket unity] applied"
        project.plugins.apply(PLUGIN_NAME)

        assert project.plugins.hasPlugin(GET_PLUGIN_NAME)
        assert project.plugins.hasPlugin(PLUGIN_NAME)

        and: "task to be tested"
        def task = project.tasks[taskName]

        expect:
        task.finalizedBy.getDependencies(task).contains(project.tasks[PaketUnityPlugin.INSTALL_TASK_NAME])

        where:
        taskName << [PaketGetPlugin.INSTALL_TASK_NAME, PaketGetPlugin.UPDATE_TASK_NAME, PaketGetPlugin.RESTORE_TASK_NAME]
    }

    @Unroll
    def 'adds unwrap task to [paket get] task #taskName'(String taskName) {
        given: "project with [paket get] applied"
        project.plugins.apply(GET_PLUGIN_NAME)

        and: "project with [paket unity] applied"
        project.plugins.apply(PLUGIN_NAME)

        assert project.plugins.hasPlugin(GET_PLUGIN_NAME)
        assert project.plugins.hasPlugin(PLUGIN_NAME)

        and: "task to be tested"
        def task = project.tasks[taskName]

        expect:
        task.finalizedBy.getDependencies(task).contains(project.tasks[PaketUnityPlugin.UNWRAP_UPM_TASK_NAME])

        where:
        taskName << [PaketUnityPlugin.INSTALL_TASK_NAME]
    }


    @Unroll
    def 'adds install task to [paket get] task #taskName 2'(String taskName) {
        given: "project with [paket unity] applied"
        project.plugins.apply(PLUGIN_NAME)

        and: "project with [paket get] applied"
        project.plugins.apply(GET_PLUGIN_NAME)

        assert project.plugins.hasPlugin(GET_PLUGIN_NAME)
        assert project.plugins.hasPlugin(PLUGIN_NAME)

        and: "task to be tested"
        def task = project.tasks[taskName]

        expect:
        task.finalizedBy.getDependencies(task).contains(project.tasks[PaketUnityPlugin.INSTALL_TASK_NAME])

        where:
        taskName << [PaketGetPlugin.INSTALL_TASK_NAME, PaketGetPlugin.UPDATE_TASK_NAME, PaketGetPlugin.RESTORE_TASK_NAME]
    }

    @Unroll
    def 'adds unwrap task to [paket get] task #taskName 2'(String taskName) {
        given: "project with [paket unity] applied"
        project.plugins.apply(PLUGIN_NAME)

        and: "project with [paket get] applied"
        project.plugins.apply(GET_PLUGIN_NAME)

        assert project.plugins.hasPlugin(GET_PLUGIN_NAME)
        assert project.plugins.hasPlugin(PLUGIN_NAME)

        and: "task to be tested"
        def task = project.tasks[taskName]

        expect:
        task.finalizedBy.getDependencies(task).contains(project.tasks[PaketUnityPlugin.UNWRAP_UPM_TASK_NAME])

        where:
        taskName << [PaketUnityPlugin.INSTALL_TASK_NAME]
    }

}

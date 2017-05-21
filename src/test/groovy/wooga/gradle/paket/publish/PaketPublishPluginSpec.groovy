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

package wooga.gradle.paket.publish

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec

class PaketPublishPluginActivationSpec extends PluginProjectSpec {
    @Override
    String getPluginName() { return 'net.wooga.paket-publish' }
}


class PaketPublishPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.paket-publish'
    public static final String BASE_PLUGIN_NAME = 'net.wooga.paket-base'
    public static final String PUBLISH_PLUGIN_NAME = 'publishing'

    def "applies plugin [paket-base]"() {
        given:
        assert !project.pluginManager.findPlugin(PLUGIN_NAME)
        assert !project.pluginManager.findPlugin(BASE_PLUGIN_NAME)

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        project.pluginManager.findPlugin(BASE_PLUGIN_NAME)
    }

    def "applies plugin [publishing]"() {
        given:
        assert !project.pluginManager.findPlugin(PLUGIN_NAME)
        assert !project.pluginManager.findPlugin(PUBLISH_PLUGIN_NAME)

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        project.pluginManager.findPlugin(PUBLISH_PLUGIN_NAME)
    }

    def 'Creates the [paket] extension'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(DefaultPaketPushPluginExtension.NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(DefaultPaketPushPluginExtension.NAME)
        extension instanceof DefaultPaketPushPluginExtension
    }
}

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

package wooga.gradle.paket.pack

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec
import org.gradle.api.plugins.BasePlugin
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.pack.tasks.PaketPack

class PaketPackPluginActivationSpec extends PluginProjectSpec {
    @Override
    String getPluginName() { return 'net.wooga.paket-pack' }
}


class PaketPackPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.paket-pack'
    public static final String BASE_PLUGIN_NAME = 'net.wooga.paket-base'
    public static final String GET_PLUGIN_NAME = 'net.wooga.paket-get'

    def "applies plugin [paket-base]"() {
        given:
        assert !project.pluginManager.findPlugin(PLUGIN_NAME)
        assert !project.pluginManager.findPlugin(BASE_PLUGIN_NAME)

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        project.pluginManager.findPlugin(BASE_PLUGIN_NAME)
    }

    def "creates no pack task after apply"() {
        given: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)

        and: "one paket template file in the file system"
        projectWithPaketTemplate(projectDir)
        assert !project.tasks.withType(PaketPack)

        expect:
        !project.tasks.withType(PaketPack)
    }

    def "creates pack task after evaluation"() {
        given: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)

        and: "one paket template file in the file system"
        projectWithPaketTemplate(projectDir)
        assert !project.tasks.withType(PaketPack)

        when:
        project.evaluate()

        then:
        def tasks = project.tasks.withType(PaketPack)
        tasks.size() == 1
        tasks[0].name == "paketPack-TestPackage"
    }

    def "creates pack tasks for template files in sub directories"() {
        given: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)

        and: "one paket template file in subdirectory in the file system"
        projectWithPaketTemplates(["Test.Package"])
        assert !project.tasks.withType(PaketPack)

        when:
        project.evaluate()

        then:
        def tasks = project.tasks.withType(PaketPack)
        tasks.size() == 1
        tasks[0].name == "paketPack-TestPackage"
    }

    def "creates multiple pack tasks"() {
        given: "some paket template files in the file system"
        projectWithPaketTemplates(["Test.Package1", "Test.Package2", "Test.Package3"])

        and: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)
        assert !project.tasks.withType(PaketPack)

        when:
        project.evaluate()

        then:
        def tasks = project.tasks.withType(PaketPack)
        tasks.size() == 3
        tasks.every { it.name =~ /paketPack-TestPackage[\d]/ }
        tasks.every { it.group == BasePlugin.BUILD_GROUP }
        tasks.every { it.dependsOn.contains(project.tasks[PaketBasePlugin.BOOTSTRAP_TASK_NAME]) }
    }

    def "adds artifact to configuration [nupkg]"() {
        given: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)

        and: "some paket template files in the file system"
        projectWithPaketTemplates(["Test.Package1", "Test.Package2", "Test.Package3"])
        assert !project.configurations[PaketBasePlugin.PAKET_CONFIGURATION].allArtifacts

        when:
        project.evaluate()

        then:
        def artifacts = project.configurations[PaketBasePlugin.PAKET_CONFIGURATION].allArtifacts
        artifacts.size() == 3
        artifacts.every { it.name =~ /Test\.Package[\d]/ }
        artifacts.every { it.file.path =~ /TestPackage[\d]\..*?\.nupkg/ }
    }

    def "set paket install dependency when plugin [paket get] is activated"() {
        given: "project with plugin applied"
        project.pluginManager.apply(PLUGIN_NAME)

        and: "plugin paket-get applied"
        project.pluginManager.apply(GET_PLUGIN_NAME)
        assert project.tasks[PaketGetPlugin.INSTALL_TASK_NAME]

        and: "some paket template files in the file system"
        projectWithPaketTemplates(["Test.Package1", "Test.Package2", "Test.Package3"])

        when:
        project.evaluate()

        then:
        def tasks = project.tasks.withType(PaketPack)
        tasks.every { it.dependsOn.contains(project.tasks[PaketGetPlugin.INSTALL_TASK_NAME]) }
    }

    def projectWithPaketTemplates(ids) {
        ids.each { String id ->
            def subDirectory = new File(projectDir, id)
            subDirectory.mkdirs()
            projectWithPaketTemplate(subDirectory, id)
        }
    }

    def projectWithPaketTemplate(File directory, String id = "Test.Package") {
        def templateFile = new File(directory, "paket.template")
        templateFile.createNewFile()
        templateFile.append("id $id")
    }
}

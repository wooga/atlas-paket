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

package wooga.gradle.paket.get

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec
import spock.lang.Unroll
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.base.tasks.AbstractPaketTask
import wooga.gradle.paket.get.tasks.PaketInstall
import wooga.gradle.paket.get.tasks.PaketRestore
import wooga.gradle.paket.get.tasks.PaketUpdate

class PaketGetPluginActivationSpec extends PluginProjectSpec {
    @Override
    String getPluginName() { return 'net.wooga.paket-get' }
}

class PaketGetPluginSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.paket-get'
    public static final String BASE_PLUGIN_NAME = 'net.wooga.paket-base'

    def "applies plugin [paket-base]"() {
        given:
        assert !project.pluginManager.findPlugin(PLUGIN_NAME)
        assert !project.pluginManager.findPlugin(BASE_PLUGIN_NAME)

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        project.pluginManager.findPlugin(BASE_PLUGIN_NAME)
    }

    def "creates runtime tasks"() {
        given:
        assert !project.pluginManager.findPlugin(PLUGIN_NAME)
        assert !project.tasks.find { it.group == PaketGetPlugin.GROUP }
        project.pluginManager.apply(BASE_PLUGIN_NAME)
        def bootstrapTask = project.tasks[PaketBasePlugin.BOOTSTRAP_TASK_NAME]

        when:
        project.pluginManager.apply(PLUGIN_NAME)

        then:
        def tasks = project.tasks.findAll { it.group == PaketGetPlugin.GROUP }
        !tasks.empty
        tasks.every { it.dependsOn.contains(bootstrapTask) }
        tasks.any { it instanceof PaketInstall }
        tasks.any { it instanceof PaketUpdate }
        tasks.any { it instanceof PaketRestore }
    }

    def "create runtime tasks with correct names"(String taskName, Class<AbstractPaketTask> taskClass) {
        given:
        project.pluginManager.apply(PLUGIN_NAME)

        expect:
        def task = (AbstractPaketTask) project.tasks[taskName]
        taskClass.isInstance(task)
        task.paketExtension

        where:
        taskName                         | taskClass
        PaketGetPlugin.INSTALL_TASK_NAME | PaketInstall.class
        PaketGetPlugin.UPDATE_TASK_NAME  | PaketUpdate.class
        PaketGetPlugin.RESTORE_TASK_NAME | PaketRestore.class
    }

    @Unroll
    def "generates update tasks for dependency in paket.dependencies #dependency"() {
        given: "a paket dependencies file"
        def paketDependencies = new File(projectDir, "paket.dependencies")
        paketDependencies << """
        source https://nuget.org/api/v2
        
        nuget Dependency.One = 0.1.0
        nuget Dependency.Two ~> 2.0.1

        """.stripIndent()

        and:
        project.pluginManager.apply(PLUGIN_NAME)

        expect:
        def task = (PaketUpdate) project.tasks["paketUpdate$dependency"]
        taskClass.isInstance(task)
        task.paketExtension
        task.nugetPackageId == dependency

        where:
        dependency            | taskClass
        "Dependency.One"      | PaketUpdate.class
        "Dependency.Two"      | PaketUpdate.class
    }
}

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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Delete
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.plugins.LifecycleBasePlugin
import wooga.gradle.paket.base.dependencies.internal.DefaultPaketDependencyHandler
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.base.repository.internal.DefaultNugetArtifactRepositoryHandlerConvention
import wooga.gradle.paket.base.tasks.PaketDependenciesTask
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask
import wooga.gradle.paket.base.tasks.PaketBootstrap
import wooga.gradle.paket.base.tasks.PaketInit

/**
 * The base {@link Plugin} for paket based plugins.
 * <p>
 * This plugin is applied by other paket plugins and adds basic shared functionality.
 */
class PaketBasePlugin implements Plugin<Project> {

    Project project

    static final String EXTENSION_NAME = 'paket'
    static final String BOOTSTRAP_TASK_NAME = "paketBootstrap"
    static final String INIT_TASK_NAME = "paketInit"
    static final String PAKET_DEPENDENCIES_TASK_NAME = "paketDependencies"
    static final String PAKET_CONFIGURATION = "nupkg"

    @Override
    void apply(Project project) {
        this.project = project

        def services = (project as ProjectInternal).getServices()
        def injector = services.get(Instantiator.class)
        def fileResolver = services.get(FileResolver.class)

        def paketDependencyHandler = new DefaultPaketDependencyHandler(project)
        ExtensionAware.cast(project.dependencies).extensions.add(EXTENSION_NAME, paketDependencyHandler)

        DefaultRepositoryHandler handler = (DefaultRepositoryHandler) project.repositories

        DefaultNugetArtifactRepositoryHandlerConvention repositoryConvention = new DefaultNugetArtifactRepositoryHandlerConvention(handler, fileResolver, injector)
        new DslObject(handler).getConvention().getPlugins().put("net.wooga.paket-base", repositoryConvention)


        final extension = project.extensions.create(EXTENSION_NAME, DefaultPaketPluginExtension, project, paketDependencyHandler)
        project.pluginManager.apply(LifecycleBasePlugin)

        configurePaketTasks(project, extension)
        addDependenciesTask(project)
        addBootstrapTask(project, extension)
        addInitTask(project, extension)
        setConfigurations(project)
        setupPaketTasks(project)
        setCleanTargets(project)
    }

    private static void setupPaketTasks(final Project project) {
        final paketBootstrapTask = project.tasks[BOOTSTRAP_TASK_NAME]

        project.tasks.withType(AbstractPaketTask, new Action<AbstractPaketTask>() {
            @Override
            void execute(AbstractPaketTask task) {
                if (!(task instanceof PaketBootstrap)) {
                    task.dependsOn paketBootstrapTask
                }
            }
        })

        project.tasks.withType(PaketInit, new Action<PaketInit>() {
            @Override
            void execute(PaketInit task) {
                task.finalizedBy paketBootstrapTask
            }
        })
    }

    private static void configurePaketTasks(final Project project, PaketPluginExtension extension) {
        project.tasks.withType(AbstractPaketTask, new Action<AbstractPaketTask>() {
            @Override
            void execute(AbstractPaketTask task) {

                final taskConvention = task.conventionMapping
                taskConvention.map("executable", { extension.getExecutable() })
                taskConvention.map("monoExecutable", { extension.getMonoExecutable() })
                taskConvention.map("logFile", { new File("${project.buildDir}/logs/${task.name}.log") })
                taskConvention.map("dependenciesFile", {

                    if(extension.getPaketDependenciesFile().exists()){
                        return project.files(extension.getPaketDependenciesFile())
                    }
                    return project.files()

                })
            }
        })
    }

    private static void addDependenciesTask(final Project project) {
        project.tasks.create(name: PAKET_DEPENDENCIES_TASK_NAME, type: PaketDependenciesTask)
    }

    private static void addInitTask(final Project project, PaketPluginExtension extension) {
        final task = project.tasks.create(name: INIT_TASK_NAME, type: PaketInit, group: TaskConfiguration.GROUP)
        task.conventionMapping.map("dependenciesFile", { extension.getPaketDependenciesFile() })
    }

    private static void addBootstrapTask(final Project project, PaketPluginExtension extension) {
        PaketBootstrap task = project.tasks.create(name: BOOTSTRAP_TASK_NAME, type: PaketBootstrap)
        task.dependsOn(project.tasks.getByName(PAKET_DEPENDENCIES_TASK_NAME))

        final taskConvention = task.conventionMapping
        taskConvention.map("executable", { extension.getBootstrapperExecutable() })
        taskConvention.map("paketExecutable", { extension.getExecutable() })
        taskConvention.map("bootstrapURL", { extension.getPaketBootstrapperUrl() })
        taskConvention.map("paketVersion", { extension.getVersion() })

        task.outputs.upToDateWhen(new Spec<PaketBootstrap>() {
            @Override
            boolean isSatisfiedBy(PaketBootstrap t) {
                if(t.paketExecutable.exists()) {
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream()

                    String osName = System.getProperty("os.name").toLowerCase()

                    def paketArgs = []

                    if (!osName.contains("windows")) {
                        paketArgs << t.getMonoExecutable()
                    }

                    paketArgs << t.paketExecutable
                    paketArgs << "--version"

                    def execResult = project.exec {
                        commandLine = paketArgs
                        standardOutput = stdOut
                        ignoreExitValue = true
                    }

                    return execResult.exitValue == 0 && stdOut.toString().contains(t.paketVersion)
                }
                return false
            }
        })
    }

    private static void setConfigurations(final Project project) {
        def configurations = project.configurations
        def configuration = configurations.maybeCreate(PAKET_CONFIGURATION)
        configuration.description = "paket nupkg archive"
        configuration.transitive = false
    }

    private static void setCleanTargets(final Project project) {
        final clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME) as Delete
        clean.delete(project.file("paket-files"), project.file("packages"), project.file(".paket"))
    }
}

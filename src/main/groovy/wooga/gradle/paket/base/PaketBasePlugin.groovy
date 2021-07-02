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
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.plugins.LifecycleBasePlugin
import wooga.gradle.paket.base.dependencies.internal.DefaultPaketDependencyHandler
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.base.repository.internal.DefaultNugetArtifactRepositoryHandlerConvention
import wooga.gradle.paket.base.tasks.DownloadPaketBootstrapper
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

    static final String TASK_GROUP_NAME = "Build Setup"

    @Override
    void apply(Project project) {
        this.project = project

        def services = (project as ProjectInternal).getServices()
        def injector = services.get(Instantiator.class)
        def fileResolver = services.get(FileResolver.class)

        def paketDependencyHandler = new DefaultPaketDependencyHandler(project)
        ExtensionAware.cast(project.dependencies).extensions.add(EXTENSION_NAME, paketDependencyHandler)

        DefaultRepositoryHandler handler = (DefaultRepositoryHandler) project.repositories

        DefaultNugetArtifactRepositoryHandlerConvention repositoryConvention = new DefaultNugetArtifactRepositoryHandlerConvention(handler, fileResolver, injector, project.objects, project.providers)
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
        final paketBootstrapTask = project.tasks.named(BOOTSTRAP_TASK_NAME)

        project.tasks.withType(AbstractPaketTask).configureEach { task ->
            if (!(task instanceof PaketBootstrap)) {
                task.dependsOn(paketBootstrapTask)
            }
        }
        project.tasks.withType(PaketInit).configureEach { task ->
            task.finalizedBy(paketBootstrapTask)
        }
    }

    private static void configurePaketTasks(final Project project, PaketPluginExtension extension) {
        project.tasks.withType(AbstractPaketTask).configureEach { task ->
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
    }

    private static void addDependenciesTask(final Project project) {
        project.tasks.register(PAKET_DEPENDENCIES_TASK_NAME, PaketDependenciesTask)
    }

    private static void addInitTask(final Project project, PaketPluginExtension extension) {
        project.tasks.register(INIT_TASK_NAME, PaketInit).configure {t ->
            t.group = TASK_GROUP_NAME
            t.conventionMapping.map("dependenciesFile", { extension.getPaketDependenciesFile() })
        }
    }

    private static void addBootstrapTask(final Project project, PaketPluginExtension extension) {
        def downloadTaskProvider = project.tasks.register("downloadPaketBootstrapper", DownloadPaketBootstrapper)
        downloadTaskProvider.configure {d ->
            final dTaskConvention = d.conventionMapping
            dTaskConvention.map("paketBootstrapperExecutable", { extension.getBootstrapperExecutable() })
            dTaskConvention.map("bootstrapURL", { extension.getPaketBootstrapperUrl() })
        }

        project.tasks.register(BOOTSTRAP_TASK_NAME, PaketBootstrap).configure { task ->
            task.dependsOn(downloadTaskProvider, project.tasks.named(PAKET_DEPENDENCIES_TASK_NAME))

            final taskConvention = task.conventionMapping
            taskConvention.map("executable", { extension.getBootstrapperExecutable() })
            taskConvention.map("paketExecutable", { extension.getExecutable() })
            taskConvention.map("paketVersion", { extension.getVersion() })

            task.outputs.upToDateWhen { PaketBootstrap t ->
                if (t.paketExecutable.exists()) {
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
        }
    }

    private static void setConfigurations(final Project project) {
        def configurations = project.configurations
        def configuration = configurations.maybeCreate(PAKET_CONFIGURATION)
        configuration.description = "paket nupkg archive"
        configuration.transitive = false
    }

    private static void setCleanTargets(final Project project) {
        final cleanProvider = project.tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME)
        cleanProvider.configure { clean ->
            clean.delete(project.file("paket-files"), project.file("packages"), project.file(".paket"))
        }
    }
}

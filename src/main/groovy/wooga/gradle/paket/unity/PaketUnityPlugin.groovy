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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.base.PaketPluginExtension
import wooga.gradle.paket.get.PaketGetPlugin
import wooga.gradle.paket.get.tasks.PaketUpdate
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension
import wooga.gradle.paket.unity.tasks.PaketUnityInstall
import wooga.gradle.paket.unity.tasks.PaketUnwrapUPMPackages

/**
 * A {@link Plugin} which adds tasks to install NuGet packages into a Unity3D project.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins {
 *         id 'net.wooga.paket-unity' version '0.10.1'
 *     }
 * }
 * </pre>
 */
class PaketUnityPlugin implements Plugin<Project> {

    Project project

    static final String GROUP = "PaketUnity"
    static final String EXTENSION_NAME = 'paketUnity'
    static final String INSTALL_TASK_NAME = "paketUnityInstall"
    static final String UNWRAP_UPM_TASK_NAME = "paketUnityUnwrapUPMPackages"
    @Override
    void apply(Project project) {
        this.project = project

        project.pluginManager.apply(PaketBasePlugin.class)
        final PaketPluginExtension baseExtension = project.extensions.getByName(PaketBasePlugin.EXTENSION_NAME) as PaketPluginExtension

        final extension = project.extensions.create(EXTENSION_NAME, DefaultPaketUnityPluginExtension, project, baseExtension.dependencyHandler)
        createPaketUnityInstallTasks(project, extension)
        createPaketUpmUnwrapTasks(project, extension)
        extension.assemblyDefinitionFileStrategy = PaketUnityPluginConventions.assemblyDefinitionFileStrategy

        project.tasks.matching({ it.name.startsWith("paketUnity")}).configureEach { task ->
            task.onlyIf {
                extension.getPaketReferencesFiles() && !extension.getPaketReferencesFiles().isEmpty() &&
                        extension.getPaketDependenciesFile() && extension.getPaketDependenciesFile().exists() &&
                        extension.getPaketLockFile() && extension.getPaketLockFile().exists()
            }
        }

        configurePaketDependencyInstallIfPresent()
    }

    private static void createPaketUnityInstallTasks(final Project project, final PaketUnityPluginExtension extension) {
        def installProviders = extension.paketReferencesFiles.files.collect { referenceFile ->
            def taskName = INSTALL_TASK_NAME + referenceFile.parentFile.name
            def installProvider = project.tasks.register(taskName, PaketUnityInstall)
            installProvider.configure { PaketUnityInstall t ->
                t.group = GROUP
                t.description = "Installs dependencies for Unity3d project ${referenceFile.parentFile.name} "
                t.conventionMapping.map("paketOutputDirectoryName", { extension.getPaketOutputDirectoryName() })
                t.conventionMapping.map("includeAssemblyDefinitions", { extension.getIncludeAssemblyDefinitions() })
                t.conventionMapping.map("includeTests", { extension.getIncludeTests() })
                t.conventionMapping.map("assemblyDefinitionFileStrategy", { extension.getAssemblyDefinitionFileStrategy() })
                t.frameworks = extension.getPaketDependencies().getFrameworks()
                t.lockFile = extension.getPaketLockFile()
                t.referencesFile = referenceFile
            }
            return installProvider
        }
        def lifecycleTaskProvider = project.tasks.register(INSTALL_TASK_NAME)

        lifecycleTaskProvider.configure {lifecycleTask ->
            lifecycleTask.group = GROUP
            lifecycleTask.description = "Installs dependencies for all Unity3d projects"
            installProviders.each { installTaskProvider ->
                lifecycleTask.dependsOn(installTaskProvider)
            }
        }
    }

    private static void createPaketUpmUnwrapTasks(final Project project, final PaketUnityPluginExtension extension) {
        def unwrapProviders = extension.paketReferencesFiles.files.collect { referenceFile ->
            def taskName = UNWRAP_UPM_TASK_NAME + referenceFile.parentFile.name
            def unwrapProvider = project.tasks.register(taskName, PaketUnwrapUPMPackages)
            unwrapProvider.configure { PaketUnwrapUPMPackages t ->
                t.group = GROUP
                t.description = "Unwraps Wrapped UPM dependencies for Unity3d project ${referenceFile.parentFile.name} "
                t.lockFile = extension.getPaketLockFile()
                t.referencesFile = referenceFile
            }
            return unwrapProvider
        }
        def lifecycleTaskProvider = project.tasks.register(UNWRAP_UPM_TASK_NAME)

        lifecycleTaskProvider.configure {lifecycleTask ->
            lifecycleTask.group = GROUP
            lifecycleTask.description = "Unwraps Wrapped UPM dependencies for all Unity3d projects"
            unwrapProviders.each { unwrapTaskProvider ->
                lifecycleTask.dependsOn(unwrapTaskProvider)
            }
        }
    }

    void configurePaketDependencyInstallIfPresent() {
        project.plugins.withType(PaketGetPlugin) {

            def paketUnityInstall = project.tasks.named(INSTALL_TASK_NAME)
            def paketUpmUnwrap = project.tasks.named(UNWRAP_UPM_TASK_NAME)
            def paketInstall = project.tasks.named(PaketGetPlugin.INSTALL_TASK_NAME)
            def paketRestore = project.tasks.named(PaketGetPlugin.RESTORE_TASK_NAME)

            Closure configClosure = { task ->
                task.finalizedBy paketUnityInstall
            }

            project.tasks.withType(PaketUpdate).configureEach(configClosure)

            [paketInstall, paketRestore].each {it.configure(configClosure) }

            project.tasks.withType(PaketUnityInstall).configureEach{ t -> t.finalizedBy(paketUpmUnwrap)}
            paketUnityInstall.configure{t -> t.finalizedBy(paketUpmUnwrap)}

        }
    }
}

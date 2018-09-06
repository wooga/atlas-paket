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

package wooga.gradle.paket.base.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import wooga.gradle.paket.base.PaketBasePlugin
import wooga.gradle.paket.base.PaketPluginExtension
import wooga.gradle.paket.base.dependencies.PaketDependencyConfiguration
import wooga.gradle.paket.base.dependencies.PaketDependencyConfigurationContainer
import wooga.gradle.paket.base.dependencies.PaketDependencyHandler
import wooga.gradle.paket.base.dependencies.PaketDependencyMacros
import wooga.gradle.paket.base.repository.NugetArtifactRepository

import java.nio.file.Path

class PaketDependenciesTask extends DefaultTask {

    protected final PaketDependencyConfigurationContainer configurationContainer
    protected final PaketDependencyMacros macros
    protected final PaketDependencyHandler dependencyHandler

    private Object output

    @OutputFile
    File getOutput() {
        project.file(output)
    }

    void setOutput(Object value) {
        this.output = path
    }

    PaketDependenciesTask output(Object path) {
        this.output = path
        this
    }

    @Nested
    private PaketDependencyMacros getMacros() {
        macros
    }

    @Nested
    private PaketDependencyConfigurationContainer getDependencyConfigurationContainer() {
        dependencyHandler.configurationContainer
    }

    @Nested
    private List<NugetArtifactRepository> getRepositories() {
        project.repositories.findAll {it instanceof NugetArtifactRepository} as List<NugetArtifactRepository>
    }

    private String content

    /**
     *
     * @return
     */
    @Optional
    @Input
    private String getContent() {
        def v = project.gradle.gradleVersion.split(/\./).collect {Integer.parseInt(it)}
        if((v[0] == 4 && v[1] >= 7) || v[0] > 4) {
            content = null
        }
        else {
            if(!content) {
                def w = new StringWriter()
                writeDependencies(w)
                content = w.toString()
            }
        }
        content
    }

    PaketDependenciesTask() {
        super()
        def extension = project.extensions.getByName(PaketBasePlugin.EXTENSION_NAME) as PaketPluginExtension
        dependencyHandler = extension.dependencyHandler
        configurationContainer = extension.dependencyHandler
        macros = extension.dependencyHandler

        output = extension.paketDependenciesFile
        onlyIf(new Spec<Task>() {
            @Override
            boolean isSatisfiedBy(Task element) {
                return !dependencyHandler.hasDependencies()
            }
        })
    }

    @TaskAction
    protected void generate() {
        writeDependenciesTo(getOutput())
    }

    protected void writeDependenciesTo(File dependencies) {
        dependencies.withWriter { Writer w -> writeDependencies(w) }
    }

    protected void writeDependencies(Writer w) {
        writeMacros(w)
        writeSources(w)
        writeGroups(w)
    }

    protected static void writeGroup(PaketDependencyConfiguration group, Writer writer, boolean printGroupStatement = true, String indent = '    ') {
        if(printGroupStatement) {
            writer.println()
            writer.println("group ${group.getName().capitalize()}")
        }

        group.all { dependency ->
            writer.println("${indent}${dependency.toString()}")
        }
    }

    protected void writeGroups(Writer writer) {
        def main = configurationContainer.getByName(PaketDependencyHandler.MAIN_GROUP)
        writeGroup(main, writer, false,'')

        configurationContainer.findAll {it.name != 'main'}.each {
            writeGroup(it, writer)
        }
    }

    protected void writeSources(Writer writer) {
        getRepositories().each { repo ->
            if(repo.url) {
                writer.print("source ${repo.url}")
                def credentials = repo.getCredentials()
                if(credentials.username && credentials.password) {
                    writer.println(" username: \"${credentials.username}\" password: \"${credentials.password}\"")
                }
                else{
                    writer.println()
                }
            } else {
                repo.dirs.each { dir ->
                    Path pathAbsolute = dir.toPath()
                    Path pathBase = getOutput().parentFile.toPath()
                    Path pathRelative = pathBase.relativize(pathAbsolute)

                    writer.println("source ${pathRelative.toString()}")
                }
            }
        }
    }

    protected void writeMacros(Writer writer) {
        def frameworks = getMacros().frameworks
        if(frameworks) {
            writer.println("framework: ${frameworks.join(", ")}")
        }
    }
}

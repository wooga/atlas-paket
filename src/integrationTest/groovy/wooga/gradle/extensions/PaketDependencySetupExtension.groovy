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

package wooga.gradle.extensions

import groovy.transform.InheritConstructors
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo

class PaketDependencySetupExtension extends AbstractAnnotationDrivenExtension<PaketDependency> {

    @Override
    void visitFieldAnnotation(PaketDependency annotation, FieldInfo field) {
        def interceptor
        interceptor = new PaketDependencyInterceptor(field.name, annotation.projectDependencies(), annotation.dependencyFiles())
        interceptor.install(field.parent.getTopSpec())
        interceptor.install(field.parent.getBottomSpec())
    }
}

@InheritConstructors
class PaketDependencyInterceptor extends GradleIntegrationSpecInterceptor implements PaketDependencySetup {

    List<String> projectDependencies
    List<String> dependencyFiles

    protected File paketDependencies
    protected File paketLock

    void setDependencyFiles(List<String> files) {
        dependencyFiles = files
    }

    PaketDependencyInterceptor(String fieldName, String[] projectDependencies, String[] dependencyFiles) {
        super(fieldName)
        this.projectDependencies = projectDependencies.toList()
        this.dependencyFiles = dependencyFiles.toList()
    }

    @Override
    File getPaketDependencies() {
        paketDependencies
    }

    @Override
    File getPaketLock() {
        paketLock
    }

    void setupProject(IMethodInvocation invocation) {
        createDependencies()
    }

    File createDependencies() {
        def dependenciesFile = createFile("paket.dependencies")
        dependenciesFile.text = ""
        dependenciesFile << """source https://nuget.org/api/v2
nuget ${projectDependencies.join("\nnuget ")}""".stripIndent()

        projectDependencies.each { dependency ->
            dependencyFiles.each {
                createFile("packages/${dependency}/content/${it}")
            }
        }

        createLockFile(projectDependencies)
        paketDependencies = dependenciesFile
        dependenciesFile
    }

    File createDependencies(List<String> dependencies) {
        projectDependencies = dependencies
        createDependencies()
    }

    void createLockFile(List<String> dependencies) {
        def lockFile = createFile("paket.lock")
        lockFile.text = ""
        lockFile << """
NUGET
    remote: https://wooga.artifactoryonline.com/wooga/api/nuget/atlas-nuget
        ${dependencies.join("\n")}""".stripIndent()
        paketLock = lockFile
    }
}
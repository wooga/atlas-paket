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
import nebula.test.IntegrationSpec
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension
import wooga.gradle.paket.unity.internal.DefaultPaketUnityPluginExtension

class PaketUnitySetupExtension extends AbstractAnnotationDrivenExtension<PaketUnity> {
    @Override
    void visitFieldAnnotation(PaketUnity annotation, FieldInfo field) {
        def interceptor
        interceptor = new PaketUnitySetupInterceptor(field.name, annotation.projectName(), annotation.projectReferences())
        interceptor.install(field.parent.getTopSpec())
        interceptor.install(field.parent.getBottomSpec())
    }
}

@InheritConstructors
class PaketUnitySetupInterceptor extends GradleIntegrationSpecInterceptor implements PaketUnitySetup {

    protected final String projectName

    File projectReferencesFile

    List<String> projectReferences

    @Override
    String getName() {
        return projectName
    }

    PaketUnitySetupInterceptor(String fieldName, String projectName, String[] projectReferences) {
        super(fieldName)
        this.projectName = projectName ?: fieldName
        this.projectReferences = projectReferences.toList()
    }

    void setupProject(IMethodInvocation invocation) {
        createOrUpdateReferenceFile()
    }

    @Override
    File getInstallDirectory() {
        new File(unityProjectDir, "/Assets/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_DIRECTORY}")
    }

    @Override
    File getUPMInstallDirectory() {
        new File(unityProjectDir, "/Packages")
    }

    @Override
    File getUnityProjectDir() {
        new File(projectDir, projectName)
    }

    File createOrUpdateReferenceFile() {
        def path = "${projectName}/${DefaultPaketUnityPluginExtension.DEFAULT_PAKET_UNITY_REFERENCES_FILE_NAME}"
        def referencesFile = createFile(path)
        referencesFile.text = """${projectReferences.join("\r")}""".stripIndent()
        this.projectReferencesFile = referencesFile
        referencesFile
    }

    File createOrUpdateReferenceFile(List<String> projectReferences) {
        this.projectReferences = projectReferences
        createOrUpdateReferenceFile()
    }
}

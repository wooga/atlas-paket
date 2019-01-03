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

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ExtensionAnnotation(PaketUnitySetupExtension)
@interface PaketUnity {
    String projectName() default ""
    String[] projectReferences() default []
}

interface PaketUnitySetup {
    File getProjectReferencesFile()
    File getInstallDirectory()
    File getUnityProjectDir()
    String getName()

    List<String> getProjectReferences()

    File createOrUpdateReferenceFile()
    File createOrUpdateReferenceFile(List<String> projectReferences)
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ExtensionAnnotation(PaketDependencySetupExtension)
@interface PaketDependency {
    String[] projectDependencies() default []
    String[] dependencyFiles() default ["ContentFile.cs"]
}

interface PaketDependencySetup {
    File getPaketDependencies()
    File getPaketLock()

    List<String> getProjectDependencies()

    List<String> getDependencyFiles()
    void setDependencyFiles(List<String> files)

    File createDependencies()
    File createDependencies(List<String> dependencies)
}

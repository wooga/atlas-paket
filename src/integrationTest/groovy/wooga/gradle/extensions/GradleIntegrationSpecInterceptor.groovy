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

import nebula.test.IntegrationSpec
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo

abstract class GradleIntegrationSpecInterceptor extends AbstractMethodInterceptor {

    protected final String fieldName
    protected IntegrationSpec spec
    File projectDir

    GradleIntegrationSpecInterceptor(String fieldName) {
        this.fieldName = fieldName
    }

    protected final IntegrationSpec getSpecification(IMethodInvocation invocation) {
        invocation.instance ?: invocation.sharedInstance
    }

    @Override
    void interceptSetupMethod(IMethodInvocation invocation) throws Throwable {
        invocation.proceed()

        if (!spec) {
            initProject(invocation)
        } else {
            setupProject(invocation)
        }
    }

    @Override
    void interceptCleanupMethod(IMethodInvocation invocation) throws Throwable {
        invocation.proceed()

        if (spec) {
            spec = null
        }
    }

    abstract void setupProject(IMethodInvocation invocation)

    final void initProject(IMethodInvocation invocation) {
        final IntegrationSpec specInstance = getSpecification(invocation)
        spec = specInstance
        projectDir = specInstance.projectDir

        specInstance."$fieldName" = this
        assert specInstance."$fieldName" == this
    }

    void install(SpecInfo spec) {
        spec.addSetupInterceptor(this)
        spec.addCleanupInterceptor(this)
    }

    //Todo think about moving this

    protected File directory(String path, File baseDir = getProjectDir()) {
        new File(baseDir, path).with {
            mkdirs()
            it
        }
    }

    protected File file(String path, File baseDir = getProjectDir()) {
        def splitted = path.split('/')
        def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/'), baseDir) : baseDir
        def file = new File(directory, splitted[-1])
        file.createNewFile()
        file
    }

    protected File createFile(String path, File baseDir = getProjectDir()) {
        File file = file(path, baseDir)
        if (!file.exists()) {
            assert file.parentFile.mkdirs() || file.parentFile.exists()
            file.createNewFile()
        }
        file
    }
}

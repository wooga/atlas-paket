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

package wooga.gradle.paket.base.tasks

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.model.Finalize
import wooga.gradle.paket.base.PaketPluginExtension

abstract class AbstractPaketTask<T extends AbstractExecTask> extends AbstractExecTask {
    static Logger logger = Logging.getLogger(AbstractPaketTask)

    @SkipWhenEmpty
    @InputFiles
    def paketDependencies = { project.projectDir.listFiles().find {it.path == "$project.projectDir/paket.dependencies"} }

    def getPaketDependencies() {
        project.files(paketDependencies)
    }

    @Internal
    def stdOut = new ByteArrayOutputStream()

    @Internal
    def stdErr = new ByteArrayOutputStream()

    @Internal
    PaketPluginExtension paketExtension

    AbstractPaketTask(Class<T> taskType) {
        super(taskType)

        super.setStandardOutput(stdOut)
        super.setErrorOutput(stdErr)
    }

    String executable

    String getExecutable() {
        paketExtension.paketExecuteablePath
    }

    @Override
    protected void exec() {
        String osName = System.getProperty("os.name").toLowerCase()
        logger.info("Detected operationg system: {}.", osName)

        def paketArgs = []
        def execArgs = args

        if (!osName.contains("windows")) {
            logger.info("Use mono: {}.", true)
            paketExtension << paketExtension.monoExecutable
        }

        paketArgs << executable
        paketArgs += execArgs

        logger.debug("Execute command {}", paketArgs.join(" "))
        args = paketArgs

        super.exec()

        if(execResult.exitValue != 0)
        {
            logger.error(stdErr.toString())
            throw new GradleException("Paket task ${name} failed")
        }

        logger.info(stdOut.toString())
    }

    @Override
    @Finalize
    AbstractExecTask setIgnoreExitValue(boolean ignoreExitValue) {
        return this
    }

    @Override
    @Finalize
    AbstractExecTask setStandardOutput(OutputStream outputStream) {
        return this
    }

    @Override
    @Finalize
    AbstractExecTask setErrorOutput(OutputStream errorStream) {
        return this
    }
}

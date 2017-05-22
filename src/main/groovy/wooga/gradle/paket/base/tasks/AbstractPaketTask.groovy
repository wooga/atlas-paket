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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import wooga.gradle.paket.base.PaketPluginExtension

import java.util.concurrent.Callable

abstract class AbstractPaketTask<T extends AbstractPaketTask> extends DefaultTask {
    static Logger logger = Logging.getLogger(AbstractPaketTask)

    private final Class<T> taskType;

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
        super()
        this.taskType = taskType
    }

    String executable

    String getExecutable() {
        if(executable == null){
            executable = paketExtension.paketExecuteablePath
        }

        if(executable == null)
        {
            null
        }
        else if (executable instanceof Callable) {
            (String) executable.call()
        } else {
            executable.toString()
        }
    }

    @Optional
    @Input
    String paketCommand

    @Input
    Collection<String> args = []

    protected void exec() {
        String osName = System.getProperty("os.name").toLowerCase()
        logger.info("Detected operationg system: {}.", osName)

        def paketArgs = []

        if (!osName.contains("windows")) {
            logger.info("Use mono: {}.", true)
            paketArgs << paketExtension.monoExecutable
        }

        paketArgs << getExecutable()

        if(paketCommand) {
            paketArgs << paketCommand
        }

        paketArgs += args

        logger.debug("Execute command {}", paketArgs.join(" "))

        def execResult = project.exec {
            commandLine = paketArgs
            standardOutput = stdOut
            errorOutput = stdErr
            ignoreExitValue = true
        }

        if(execResult.exitValue != 0)
        {
            logger.error(stdErr.toString())
            throw new GradleException("Paket task ${name} failed")
        }

        logger.info(stdOut.toString())
    }
}

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

package wooga.gradle.paket.base.tasks.internal

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

abstract class AbstractPaketTask<T extends AbstractPaketTask> extends ConventionTask {

    static Logger logger = Logging.getLogger(AbstractPaketTask)

    @Internal
    private final Class<T> taskType

    @Internal
    protected Boolean supportLogfile = true

    @InputFile
    File executable

    @Optional
    @Input
    String monoExecutable

    private Object logFile

    @Optional
    @OutputFile
    File getLogFile() {
        return logFile ? project.file(logFile) : null
    }

    void setLogFile(Object value) {
        this.logFile = value
    }

    @Optional
    @Input
    protected String paketCommand

    @SkipWhenEmpty
    @InputFiles
    FileCollection dependenciesFile

    @Internal
    protected ByteArrayOutputStream stdOut

    @Internal
    protected ByteArrayOutputStream stdErr

    protected ArrayList<String> arguments = []

    /**
     * Returns the arguments for the command to be executed. Defaults to an empty list.
     */
    @Input
    List<String> getArgs() {
        arguments
    }

    /**
     * Adds arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    T args(Object... args) {
        args.each {
            arguments.add(it.toString())
        }

        T.cast(this)
    }

    /**
     * Adds arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    T args(Iterable<?> args) {
        args.each {
            arguments.add(it.toString())
        }

        T.cast(this)
    }

    /**
     * Sets the arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    T setArgs(Iterable<?> args) {
        arguments.clear()
        this.args(args)
    }

    AbstractPaketTask(Class<T> taskType) {
        super()
        this.taskType = taskType
        stdOut = new ByteArrayOutputStream()
        stdErr = new ByteArrayOutputStream()
    }

    @TaskAction
    protected void performPaketCommand(IncrementalTaskInputs inputs) {
        configureArguments()
        exec()
    }

    protected void configureArguments() {}

    protected final void exec() {
        String osName = System.getProperty("os.name").toLowerCase()
        logger.info("Detected operation system: {}.", osName)

        def paketArgs = []

        if (!osName.contains("windows")) {
            logger.info("Use mono: {}.", true)
            paketArgs << getMonoExecutable()
        }

        paketArgs << getExecutable()

        if (paketCommand) {
            paketArgs << paketCommand
        }

        if (supportLogfile) {
            paketArgs << "--verbose"
            paketArgs << "--log-file" << getLogFile().path
        }

        paketArgs += args

        logger.debug("Execute command {}", paketArgs.join(" "))

        def execResult = project.exec {
            commandLine = paketArgs
            standardOutput = this.stdOut
            errorOutput = this.stdErr
            ignoreExitValue = true
        }

        if (execResult.exitValue != 0) {
            logger.error(stdErr.toString())
            throw new GradleException("Paket task ${name} failed" + stdErr.toString())
        }

        logger.info(stdOut.toString())
    }
}

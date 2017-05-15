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

package net.wooga.gradle.tasks

import net.wooga.gradle.PaketPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.concurrent.Callable

class PaketBootstrap extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketBootstrap)

    @OutputFile
    def paketFile

    File getPaketFile() {
        project.file paketFile
    }

    @InputFile
    def paketBootstrapper

    File getPaketBootstrapper() {
        project.file paketBootstrapper
    }

    @Optional
    @Input
    def paketVersion

    String getPaketVersion() {
        if (paketVersion instanceof Callable) {
            paketVersion.call()
        } else {
            paketVersion.toString()
        }
    }

    PaketPluginExtension paketExtension

    @TaskAction
    void exec(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            project.delete(paketFile)
        }

        inputs.outOfDate { change ->
            def bootstrapCommandline = []
            String osName = System.getProperty("os.name").toLowerCase()
            logger.info("Detected operationg system: {}.", osName)

            if (!osName.contains("windows")) {

                bootstrapCommandline << paketExtension.monoExecutable
                logger.info("Use mono: {}.", true)
            }

            bootstrapCommandline << paketBootstrapper.path

            logger.info("requesting paket version: {}", paketVersion )

            bootstrapCommandline << paketVersion
            bootstrapCommandline << "-s"

            logger.debug("Execute command {}", bootstrapCommandline.join(" "))

            project.exec {
                commandLine = bootstrapCommandline
                standardOutput = new ByteArrayOutputStream()
            }
        }
    }
}

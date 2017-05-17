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

package wooga.gradle.paket.tasks

import wooga.gradle.paket.PaketPlugin
import wooga.gradle.paket.PaketPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty

abstract class PaketTask extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketTask)

    @SkipWhenEmpty
    @InputFiles
    def paketDependencies = { project.projectDir.listFiles().findAll {it.name == "paket.dependencies"} }

    FileCollection getPaketDependencies() {
        project.files(paketDependencies)
    }

    @Internal
    def paketCommandline

    @Internal
    PaketPluginExtension paketExtension

    PaketPluginExtension getPaketExtension() {
        if(!paketExtension) {
            paketExtension = project.extensions.findByName(PaketPlugin.WOOGA_PAKET_EXTENSION_NAME)
        }

        return paketExtension
    }

    void performPaketCommand(cl) {
        String osName = System.getProperty("os.name").toLowerCase()
        logger.info("Detected operationg system: {}.", osName)

        paketCommandline = []

        if (!osName.contains("windows")) {

            paketCommandline << getPaketExtension().monoExecutable
            logger.info("Use mono: {}.", true)
        }

        paketCommandline << getExecuteable()

        cl(paketCommandline)

        logger.debug("Execute command {}", paketCommandline.join(" "))

        def outputStream = new ByteArrayOutputStream()
        def commandOut = project.exec {
            commandLine = paketCommandline
            standardOutput = outputStream
        }

        logger.info(outputStream.toString())
    }

    @Internal
    String getExecuteable() {
        getPaketExtension().paketExecuteablePath
    }
}

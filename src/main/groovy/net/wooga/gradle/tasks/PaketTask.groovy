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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal

abstract class PaketTask extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketTask)

    @Internal
    def paketCommandline = []

    @Internal
    PaketPluginExtension paketExtension

    void performPaketCommand(cl) {
        String osName = System.getProperty("os.name").toLowerCase()
        logger.info("Detected operationg system: {}.", osName)

        if (!osName.contains("windows")) {

            paketCommandline << paketExtension.monoExecutable
            logger.info("Use mono: {}.", true)
        }

        paketCommandline << paketExtension.paketExecuteablePath

        cl(paketCommandline)

        logger.debug("Execute command {}", paketCommandline.join(" "))

        def outputStream = new ByteArrayOutputStream()
        def commandOut = project.exec {
            commandLine = paketCommandline
            standardOutput = outputStream
        }

        logger.info(outputStream.toString())
    }
}

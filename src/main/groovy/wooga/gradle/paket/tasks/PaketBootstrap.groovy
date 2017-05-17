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

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.concurrent.Callable

class PaketBootstrap extends PaketTask {

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

    @Override
    String getExecuteable() {
        getPaketBootstrapper()
    }

    @TaskAction
    void performBootstrap(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            project.delete(paketFile)
        }

        inputs.outOfDate { change ->

            performPaketCommand() {cmd ->
                logger.info("requesting paket version: {}", paketVersion )
                cmd << paketVersion
                cmd << "--prefer-nuget"
                cmd << "-s"
            }
        }
    }
}

/*
 * Copyright 2017 Wooga GmbH
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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A helper task to create an empty {@code paket.dependencies} file.
 */
class PaketInit extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketInit)

    @OutputFile
    def dependenciesFile

    PaketInit() {
        super()
        description = "Creates an empty paket.dependencies file in the working directory."

        onlyIf {
            !getDependenciesFile().exists()
        }
    }

    @TaskAction
    protected void performPaketCommand() {
        logger.info("Create empty file {}", getDependenciesFile().path)
        getDependenciesFile().createNewFile()
    }
}

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

package wooga.gradle.paket.get.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import wooga.gradle.paket.base.PaketBasePlugin

class PaketInit extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketInit)

    @OutputFile
    def dependenciesFile = "$project.projectDir/${PaketBasePlugin.DEPENDENCIES_FILE_NAME}"

    File getDependenciesFile() {
        project.file dependenciesFile
    }

    PaketInit() {
        super()
        description = "Creates an empty paket.dependencies file in the working directory."

        onlyIf {
            !getDependenciesFile().exists()
        }
    }

    @TaskAction
    void performInit() {
        logger.info("Create empty file {}", getDependenciesFile().path)
        getDependenciesFile().createNewFile()
    }
}

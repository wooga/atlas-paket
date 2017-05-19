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

import org.gradle.api.tasks.*

import java.util.concurrent.Callable

class PaketPack extends PaketTask {

    @Optional
    @Input
    def version

    String getVersion() {
        if (!version) {
            return null
        }

        if (version instanceof Callable) {
            version.call()
        } else {
            version.toString()
        }
    }

    @Optional
    @InputFile
    def templateFile

    File getTemplateFile() {
        project.file templateFile
    }

    @OutputDirectory
    def outputDir

    File getOutputDir() {
        project.file outputDir
    }

    @TaskAction
    void performPack() {

        performPaketCommand() { cmd ->
            cmd << "pack"
            cmd << "output" << getOutputDir()

            if (getVersion() != null) {
                cmd << "version" << getVersion()
            }

            if (getTemplateFile() != null) {
                cmd << "templatefile" << getTemplateFile()
            }
        }
    }
}

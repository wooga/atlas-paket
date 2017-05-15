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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable

class PaketBootstrapDownload extends DefaultTask {

    static Logger logger = Logging.getLogger(PaketBootstrapDownload)

    PaketPluginExtension paketExtension

    @OutputDirectory
    def outputDir

    File getOutputDir() {
        project.file outputDir
    }

    @Input
    def paketBootstrapperFileName

    String getPaketBootstrapperFileName() {
        if (paketBootstrapperFileName instanceof Callable) {
            paketBootstrapperFileName.call()
        } else {
            paketBootstrapperFileName.toString()
        }
    }

    @Input
    def bootstrapURL

    String getBootstrapURL() {
        if (bootstrapURL instanceof Callable) {
            bootstrapURL.call()
        } else {
            bootstrapURL.toString()
        }
    }

    @TaskAction
    void downloadBootstrapper() {
        def paketBootstrapperFile = project.file("${getOutputDir()}/${getPaketBootstrapperFileName()}")
        new URL("${getBootstrapURL()}").withInputStream { i ->
            paketBootstrapperFile.withOutputStream {
                it << i
            }
        }

    }
}

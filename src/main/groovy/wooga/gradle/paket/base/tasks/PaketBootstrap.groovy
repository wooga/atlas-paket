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

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import javax.inject.Inject
import java.util.concurrent.Callable

class PaketBootstrap extends AbstractPaketTask {
    static Logger logger = Logging.getLogger(PaketBootstrap)

    @Inject
    PaketBootstrap() {
        super(PaketBootstrap.class)
        supportLogfile = false
    }

    PaketBootstrap(Class<AbstractPaketTask> taskType) {
        super(taskType)
        supportLogfile = false
    }

    def outputDir

    def getOutputDir() {
        (File) project.file(outputDir)
    }

    @OutputFiles
    FileCollection getOutputFiles() {
        return project.files(getPaketFile(), getPaketBootstrapper())
    }

    @Input
    def paketFile

    def getPaketFile() {
        (File) project.file(paketFile)
    }

    @Input
    def paketBootstrapper

    def getPaketBootstrapper() {
        return (File) project.file(paketBootstrapper)
    }

    @Input
    def paketBootstrapperFileName

    def getPaketBootstrapperFileName() {
        if (paketBootstrapperFileName == null) {
            null
        } else if (paketBootstrapperFileName instanceof Callable) {
            (String) paketBootstrapperFileName.call()
        } else {
            paketBootstrapperFileName.toString()
        }
    }

    @Input
    def bootstrapURL

    def getBootstrapURL() {
        if (bootstrapURL == null) {
            null
        } else if (bootstrapURL instanceof Callable) {
            bootstrapURL.call()
        } else {
            bootstrapURL.toString()
        }
    }

    @Optional
    @Input
    def paketVersion

    def getPaketVersion() {
        if (paketVersion == null) {
            null
        } else if (paketVersion instanceof Callable) {
            (String) paketVersion.call()
        } else {
            paketVersion.toString()
        }
    }

    @Override
    String getExecutable() {
        this.getPaketBootstrapper()
    }

    @Override
    protected void performPaketCommand(IncrementalTaskInputs inputs) {
        checkBootstrapper()

        if (!inputs.incremental) {
            project.delete(paketFile)
        }

        inputs.outOfDate { change ->
            super.performPaketCommand(inputs)
        }
    }

    @Override
    protected void configureArguments() {
        super.configureArguments()
        logger.info("requesting paket version: {}", getPaketVersion())

        args << getPaketVersion()
        args << "--prefer-nuget"
        args << "-v"
    }

    def checkBootstrapper() {
        File f = getPaketBootstrapper()
        if (f.exists()) {
            logger.info("Bootstrap file {} already exists", f.path)
            return
        }

        new URL("${getBootstrapURL()}").withInputStream { i ->
            f.withOutputStream {
                it << i
            }
        }

        if (!f.exists()) {
            throw new GradleException("Failed to download bootstrapper from ${f.path}")
        }
    }
}

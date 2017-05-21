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

import org.gradle.api.GradleException
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
    }

    PaketBootstrap(Class<AbstractPaketTask> taskType) {
        super(taskType)
    }

    @OutputDirectory
    def outputDir

    def getOutputDir() {
        (File) project.file(outputDir)
    }


    def paketFile

    def getPaketFile() {
        (File) project.file(paketFile)
    }

    @OutputFiles
    def outputFiles = project.files(paketFile, paketBootstrapper)

    @Input
    def paketBootstrapper

    def getPaketBootstrapper() {
        (File) project.file(paketBootstrapper)
    }

    @Input
    def paketBootstrapperFileName

    def getPaketBootstrapperFileName() {
        if(paketBootstrapperFileName == null)
        {
            null
        }
        else if (paketBootstrapperFileName instanceof Callable) {
            (String) paketBootstrapperFileName.call()
        } else {
            paketBootstrapperFileName.toString()
        }
    }

    @Input
    def bootstrapURL

    def getBootstrapURL() {
        if(bootstrapURL == null)
        {
            null
        }
        else if (bootstrapURL instanceof Callable) {
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
        }
        else if (paketVersion instanceof Callable) {
            (String) paketVersion.call()
        } else {
            paketVersion.toString()
        }
    }

    @Override
    String getExecutable() {
        getPaketBootstrapper()
    }

    @TaskAction
    void performBootstrap(IncrementalTaskInputs inputs) {
        checkBootstrapper()

        if (!inputs.incremental) {
            project.delete(paketFile)
        }

        inputs.outOfDate { change ->
            exec()
        }
    }

    @Override
    protected void exec() {
        def packArguments = []
        logger.info("requesting paket version: {}", paketVersion)

        packArguments << paketVersion
        packArguments << "--prefer-nuget"
        packArguments << "-s"

        setArgs(packArguments)
        super.exec()
    }

    def checkBootstrapper() {
        if(paketBootstrapper.exists())
        {
            logger.info("Bootstrap file {} already exists", paketBootstrapper.path)
            return
        }

        new URL("${getBootstrapURL()}").withInputStream { i ->
            paketBootstrapper.withOutputStream {
                it << i
            }
        }

        if(!paketBootstrapper.exists())
        {
            throw new GradleException("Failed to download bootstrapper from ${paketBootstrapper.path}")
        }
    }
}

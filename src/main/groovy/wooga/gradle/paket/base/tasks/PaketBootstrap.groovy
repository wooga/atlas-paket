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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask

import javax.inject.Inject

class PaketBootstrap extends AbstractPaketTask {
    static Logger logger = Logging.getLogger(PaketBootstrap)

    @Input
    String bootstrapURL

    @Optional
    @Input
    String paketVersion

    @OutputFiles
    FileCollection getOutputFiles() {
        return project.files(getExecutable())
    }
    
    void setPaketVersion(String value) {
           paketVersion = value
    }

    @Inject
    PaketBootstrap() {
        super(PaketBootstrap.class)
        supportLogfile = false
    }

    PaketBootstrap(Class<AbstractPaketTask> taskType) {
        super(taskType)
        supportLogfile = false
    }

    @Override
    protected void performPaketCommand(IncrementalTaskInputs inputs) {
        checkBootstrapper()
        super.performPaketCommand(inputs)
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
        File f = getExecutable()
        if (f.exists()) {
            logger.info("Bootstrap file {} already exists", f.path)
            return
        }

        f.parentFile.mkdirs()

        def url =  new URL(getBootstrapURL())
        url.withInputStream { i ->
            f.withOutputStream {
                it << i
            }
        }

        if (!f.exists()) {
            throw new GradleException("Failed to download bootstrapper from ${f.path}")
        }
    }
}

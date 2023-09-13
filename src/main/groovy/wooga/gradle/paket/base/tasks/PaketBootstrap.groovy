/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask

import javax.inject.Inject

/**
 * A worker task to download the {@code paket.exe} file before executing any other paket tasks.
 * All paket tasks depend on this task to check for the availability and update of the {@code paket.exe}.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     paketBootstrap {
 *         paketVersion = "1.0.0"
 *         bootstrapURL = "http://...."
 *     }
 * }
 * </pre>
 */
class PaketBootstrap extends AbstractPaketTask {

    @Optional
    @Input
    String paketVersion

    @OutputFile
    File paketExecutable

    @OutputFiles
    FileCollection getOutputFiles() {
        return project.files(getPaketExecutable())
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
    protected void configureArguments() {
        super.configureArguments()
        logger.info("requesting paket version: {}", getPaketVersion())

        args << getPaketVersion()
        args << "--prefer-nuget"
        args << "-v"
    }
}

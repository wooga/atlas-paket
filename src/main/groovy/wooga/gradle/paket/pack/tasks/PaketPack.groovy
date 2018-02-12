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

package wooga.gradle.paket.pack.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.*
import wooga.gradle.paket.PaketCommand
import wooga.gradle.paket.base.tasks.AbstractPaketTask
import wooga.gradle.paket.base.utils.PaketTemplate

import java.util.concurrent.Callable

class PaketPack extends AbstractPaketTask {

    @Internal
    def packageId

    @Optional
    @Input
    String version


    @Optional
    @InputFile
    def templateFile

    File getTemplateFile() {
        project.file templateFile
    }

    @OutputDirectory
    File outputDir

    @OutputFile
    File getOutputFile() {
        def templateReader = new PaketTemplate(getTemplateFile())
        def packageID = templateReader.getPackageId()
        project.file({"$outputDir/${packageID}.${getVersion()}.nupkg"})
    }

    PaketPack() {
        super(PaketPack.class)
        paketCommand = PaketCommand.PACK
    }

    @Override
    protected void configureArguments() {
        super.configureArguments()
        args << "output" << getOutputDir().path

        if (getVersion() != Project.DEFAULT_VERSION) {
            args << "version" << getVersion()
        }

        if (getTemplateFile() != null) {
            args << "templatefile" << getTemplateFile().path
        }
    }
}

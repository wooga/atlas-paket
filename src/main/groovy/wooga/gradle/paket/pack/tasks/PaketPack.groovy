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
import wooga.gradle.paket.internal.PaketCommand
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask
import wooga.gradle.paket.base.utils.internal.PaketTemplate
import java.util.concurrent.Callable

/**
 * A task to invoke {@code paket pack} command with given {@code paket.template} file.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     task pack(type:wooga.gradle.paket.pack.tasks.PaketPack) {
 *         templateFile = file('test/paket.template')
 *         version = "1.0.0"
 *         outputDir = file('build/out')
 *     }
 * }
 * </pre>
 */
class PaketPack extends AbstractPaketTask {

    /**
     * @return the packageId of the nuget to be packed.
     */
    String getPackageId() {
        new PaketTemplate(getTemplateFile()).getPackageId()
    }

    /**
     * The nuget package version.
     */
    @Input
    String getVersion() {
        if (!version) {
            return null
        }

        if (version instanceof Callable) {
            version = version.call()
        }

        version.toString()
    }

    private Object version

    void setVersion(Object value) {
        this.version = value
    }

    @Optional
    @InputFile
    def templateFile

    /**
     * Returns the {@link File} path to the {@code paket.template} file.
     *
     * @return the template file
     */
    File getTemplateFile() {
        project.file templateFile
    }

    @OutputDirectory
    File outputDir

    /**
     * Returns the output file path for the packed nuget package.
     *
     * @return  output path for the nuget package
     * @default {@code "$outputDir/${packageID}.${version}.nupkg"}
     */
    @OutputFile
    File getOutputFile() {
        project.file({"$outputDir/${getPackageId()}.${getVersion()}.nupkg"})
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

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

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class PaketPack extends PaketTask {

    @InputDirectory
    def FileTree inputFiles

    def inputFiles(FileTree files) {
        inputFiles = files
    }

    @OutputDirectory
    def File outputDir

    @Input
    def packageVersion


    PaketPack() {
        super()
        this.description = "Packs all paket.template files given"
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        checkPaket()
        println inputs.incremental ? "CHANGED inputs considered out of date"
                : "ALL inputs considered out of date"
        if (!inputs.incremental)
            project.delete(outputDir.listFiles())

        inputs.outOfDate { change ->
            println "out of date: ${change.file}"

            def commandLine = "${runtime}.paket/paket.exe pack output ${outputDir} version ${packageVersion} templatefile ${change.file}"
            println commandLine
            println commandLine.execute().text
        }
    }
}
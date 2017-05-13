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
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class PaketPush extends PaketTask {

    @InputDirectory
    def FileTree inputFiles

    def inputFiles(FileTree files) {
        inputFiles = files
    }

    @Input
    def url

    PaketPush() {
        super()
        this.description = "Pushes the given .nupkg file."
    }

    @TaskAction
    void push(IncrementalTaskInputs inputs) {
        checkPaket()
        inputs.outOfDate { change ->
            println "out of date: ${change.file}"
            def command = "${runtime}.paket/paket.exe push url ${url} file ${change.file}"
            println command
            println command.execute().text
        }
    }
}

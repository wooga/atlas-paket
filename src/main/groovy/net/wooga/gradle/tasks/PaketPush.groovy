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

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
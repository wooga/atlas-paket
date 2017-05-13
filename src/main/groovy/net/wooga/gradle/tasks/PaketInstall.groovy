package net.wooga.gradle.tasks

import org.gradle.api.tasks.TaskAction

class PaketInstall extends PaketTask {

    PaketInstall() {
        super()
        description = 'Download the dependencies specified by the paket.dependencies or paket.lock file into the packages/ directory and update projects.'
    }

    @TaskAction
    void install() {
        checkPaket()
        def command = "${runtime}.paket/paket.exe install"
        println command
        println command.execute().text

        command = "${runtime}.paket/paket.unity3d.exe install"
        println command
        println command.execute().text
    }
}
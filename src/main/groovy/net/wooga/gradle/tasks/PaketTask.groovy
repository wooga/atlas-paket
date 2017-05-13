package net.wooga.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input

class PaketTask extends DefaultTask {

    @Input
    protected String runtime

    PaketTask() {
        super()
        group = 'Paket'
    }

    @Override
    Task configure(Closure closure) {
        String osName = System.getProperty("os.name").toLowerCase();
        runtime = (osName.contains("windows")) ? "" : "mono "
        return super.configure(closure)
    }

    void checkPaket() {
        def paketUnityCmd = new File('.paket/paket.unity3d.exe')
        if (! paketUnityCmd.exists()) {
            println("Install paket.unity3d.exe")
            println "${runtime}.paket/paket.unity3d.bootstrapper.exe".execute().text
        }
    }
}

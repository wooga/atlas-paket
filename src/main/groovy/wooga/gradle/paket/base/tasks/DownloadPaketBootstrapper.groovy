package wooga.gradle.paket.base.tasks

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class DownloadPaketBootstrapper extends ConventionTask {
    @Input
    String bootstrapURL

    @OutputFile
    File paketBootstrapperExecutable

    @TaskAction
    protected void exec() {
        File f = getPaketBootstrapperExecutable()
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

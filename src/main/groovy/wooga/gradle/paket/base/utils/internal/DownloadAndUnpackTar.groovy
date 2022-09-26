package wooga.gradle.paket.base.utils.internal

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class DownloadAndUnpackTar {
    static Logger logger = Logging.getLogger(UnwrapUpm)

    final private URL url;
    final private File destinationDir;

    DownloadAndUnpackTar(String url, File destinationDir)
    {
        this.url = new URL(url)
        this.destinationDir = destinationDir;
    }

    DownloadAndUnpackTar(PaketUPMWrapperReference ref, File outputDirectory)
    {
        this(ref.upmPackageURL, new File(outputDirectory, ref.upmPackageName))
    }

    private static File getTempTarLocation()
    {
        def f = File.createTempFile("wrapped_upm", "tgz")
        f.deleteOnExit()
        return f
    }

    void exec(Project project)
    {
        File f = getTempTarLocation()

        url.withInputStream { i ->
            f.withOutputStream {
                it << i
            }
        }

        if (!f.exists()) {
            throw new GradleException("Failed to download upm package tarball from ${url} into ${f.path}")
        }

        logger.info("downloaded ${url} into ${f.path}")

        var unwrapCommand = new UnwrapUpm(f, destinationDir);
        unwrapCommand.exec(project);

        // delete tar after it got unpacked
        f.delete()
    }
}

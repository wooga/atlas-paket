package wooga.gradle.paket.base.utils.internal

import org.gradle.api.GradleException
import org.gradle.api.Project

class DownloadAndUnpackTar {
    private String url;
    private File destinationDir;

    private static final String tempTarName = "temp.tgz"

    DownloadAndUnpackTar(String url, File destinationDir)
    {
        this.url = url
        this.destinationDir = destinationDir;
    }

    DownloadAndUnpackTar(PaketUPMWrapperReference ref, File outputDirectory)
    {
        this(ref.upmPackageURL, new File(outputDirectory, ref.upmPackageName))
    }

    private File getTempTarLocation()
    {
        return new File(this.destinationDir.getParentFile(), tempTarName);
    }

    public void exec(Project project)
    {
        File f = getTempTarLocation()
        if (f.exists()) {
            f.delete();
        }

        f.parentFile.mkdirs()

        def url =  new URL(this.url)
        url.withInputStream { i ->
            f.withOutputStream {
                it << i
            }
        }

        if (!f.exists()) {
            throw new GradleException("Failed to download upm package tarball from ${this.url} into ${f.path}")
        }

        project.logger.info("downloaded ${this.url} into ${f.path}")

        var unwrapCommand = new UnwrapUpm(f, destinationDir);
        unwrapCommand.exec(project);

        // delete tar after it got unpacked
        f.delete()
    }
}

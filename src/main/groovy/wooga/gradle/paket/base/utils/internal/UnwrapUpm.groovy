package wooga.gradle.paket.base.utils.internal

import org.apache.commons.io.FileUtils
=import org.gradle.api.GradleException
import org.gradle.api.Project

class UnwrapUpm {
    private File upmTarFile;
    private File destinationDir;

    // this is how Unity names the root directory inside the upm tar files
    private static final String unpackDirectoryName = "package"

    UnwrapUpm(File tarFile, File destinationDir)
    {
        this.upmTarFile = tarFile
        this.destinationDir = destinationDir;
    }

    UnwrapUpm(String url, File destinationDir, Project project)
    {
        this(project.file(url), destinationDir)
    }

    UnwrapUpm(PaketUPMWrapperReference ref, File outputDirectory)
    {
        this(ref.upmPackageTar, new File(outputDirectory, ref.upmPackageName))
    }

    public void exec(Project project)
    {
        if (!upmTarFile.exists()) {
            throw new GradleException("No upm package tarball found in ${upmTarFile.path}")
        }

        project.logger.info("unpacking ${this.upmTarFile.path} into ${destinationDir.path}")

        project.tarTree(this.upmTarFile).files.each { fileFromTar ->
            def path = fileFromTar.toPath()
            def indexOfPackage = 0

            // looks for the first directory that is called "package"
            while (indexOfPackage < path.getNameCount() && path.getName(indexOfPackage).toString() != unpackDirectoryName) {
                indexOfPackage++;
            }

            def relativePath = path.subpath(indexOfPackage+1, path.getNameCount())

            project.logger.debug("copyfile ${fileFromTar.path} into ${new File(destinationDir, relativePath.toString()).path}")

            FileUtils.copyFile(fileFromTar, new File(destinationDir, relativePath.toString()))
        }
   }
}

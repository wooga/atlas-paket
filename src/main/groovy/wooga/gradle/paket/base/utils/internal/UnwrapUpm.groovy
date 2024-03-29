package wooga.gradle.paket.base.utils.internal

import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class UnwrapUpm {

    static Logger logger = Logging.getLogger(UnwrapUpm)

    private File upmTarFile;
    private File destinationDir;

    // this is how Unity names the root directory inside the upm tar files
    private static final String unpackDirectoryName = "package"

    UnwrapUpm(File tarFile, File destinationDir)
    {
        this.upmTarFile = tarFile
        this.destinationDir = destinationDir;
    }

    UnwrapUpm(PaketUPMWrapperReference ref, File outputDirectory)
    {
        this(ref.upmPackageTar, new File(outputDirectory, ref.upmPackageName))
    }

    void exec(Project project)
    {
        if (!upmTarFile.exists()) {
            throw new GradleException("No upm package tarball found in ${upmTarFile.path}")
        }

        logger.info("unpacking ${this.upmTarFile.path} into ${destinationDir.path}")

        project.tarTree(this.upmTarFile).files.each { fileFromTar ->
            def path = fileFromTar.toPath()
            def indexOfPackage = 0

            // looks for the first directory that is called "package"
            while (indexOfPackage < path.getNameCount() && path.getName(indexOfPackage).toString() != unpackDirectoryName) {
                indexOfPackage++;
            }

            def relativePath = path.subpath(indexOfPackage+1, path.getNameCount())

            logger.debug("copyfile ${fileFromTar.path} into ${new File(destinationDir, relativePath.toString()).path}")

            FileUtils.copyFile(fileFromTar, new File(destinationDir, relativePath.toString()))
        }
   }
}

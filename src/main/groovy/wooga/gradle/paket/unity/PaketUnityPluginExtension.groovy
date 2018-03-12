package wooga.gradle.paket.unity

import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.PaketPluginExtension

/**
 * A extension point for paket unity
 */
interface PaketUnityPluginExtension extends PaketPluginExtension {

    /**
     * Returns a {@link FileCollection} object containing all {@code paket.unity3D.references} files.
     *
     * @return a collection of all {@code paket.unity3D.references} files.
     */
    FileCollection getPaketReferencesFiles()

    /**
     * @return  the paket unity output directory name
     */
    String getPaketOutputDirectoryName()

    /**
     * Sets the paket unity output directory name
     * @param directory name of the output directory
     */
    void setPaketOutputDirectoryName(String directory)

    /**
     * @return a {@link File} path to the {@code paket.lock} file in the project.
     */
    File getPaketLockFile()
}

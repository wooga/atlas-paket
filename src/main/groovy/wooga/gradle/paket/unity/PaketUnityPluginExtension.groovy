package wooga.gradle.paket.unity

import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.PaketPluginExtension

interface PaketUnityPluginExtension extends PaketPluginExtension {
    FileCollection getPaketReferencesFiles()

    String getGetPaketOutputDirectoryName()

    void setGetPaketOutputDirectoryName(String directory)
}

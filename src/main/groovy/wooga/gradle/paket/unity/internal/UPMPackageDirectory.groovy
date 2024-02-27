package wooga.gradle.paket.unity.internal

import groovy.json.JsonOutput

/**
 * An UPM package. For details see:  https://docs.unity3d.com/Manual/CustomPackages.html
 */
class UPMPackageDirectory {

    final File baseDir
    final String name

    /**
     * @return True if the directory exists in the file system
     */
    boolean exists () {
        baseDir.exists()
    }

    /**
     * @return A reference to the manifest file
     */
    Optional<File> getManifestFile() {
        return Optional.ofNullable(new File(baseDir, "package.json")).map {
            it.exists()? it : null
        }
    }

    UPMPackageDirectory(File paketPackageDir) {
        this.baseDir = paketPackageDir
        this.name = baseDir.name
    }

    File writeManifest(Map<String, Object> contents) {
        def createdPkgJson = new File(baseDir, "package.json")
        createdPkgJson << JsonOutput.prettyPrint(JsonOutput.toJson(contents))
        return createdPkgJson
    }
}

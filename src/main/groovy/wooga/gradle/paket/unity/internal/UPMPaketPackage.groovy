package wooga.gradle.paket.unity.internal

import groovy.json.JsonOutput

class UPMPaketPackage {

    final File baseDir
    final String name

    UPMPaketPackage(File paketPackageDir) {
        this.baseDir = paketPackageDir
        this.name = baseDir.name
    }

    Optional<File> getPackageManifest() {
        return Optional.ofNullable(new File(baseDir, "package.json")).map {
            it.exists()? it : null
        }
    }

    File writePackageManifest(Map<String, Object> contents) {
        return writePackageManifest(baseDir, contents)
    }

    static File writePackageManifest(File baseDir, Map<String, Object> contents) {
        def createdPkgJson = new File(baseDir, "package.json")
        createdPkgJson << JsonOutput.prettyPrint(JsonOutput.toJson(contents))
        return createdPkgJson
    }

    static Map<String, Object> basicUPMPackageManifest(String packageName, String displayName, Map<String, Object> overrides = [:]) {
        Map<String, Object> base = [
                name: packageName,
                displayName: displayName,
                version: "0.0.0"
        ]
        base.putAll(overrides)
        return base
    }

}

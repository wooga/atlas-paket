package wooga.gradle.paket.unity.internal

import groovy.json.JsonOutput

class UPMPaketPackage {

    final File baseDir
    final String name

    UPMPaketPackage(File paketPackageDir) {
        this.baseDir = paketPackageDir
        this.name = baseDir.name
    }

    Optional<File> getPackageDotJson() {
        return Optional.ofNullable(new File(baseDir, "package.json")).map {
            it.exists()? it : null
        }
    }

    File writePackageJson(Map<String, Object> contents) {
        return writePackageJson(baseDir, contents)
    }

    static File writePackageJson(File baseDir, Map<String, Object> contents) {
        def createdPkgJson = new File(baseDir, "package.json")
        createdPkgJson << JsonOutput.prettyPrint(JsonOutput.toJson(contents))
        return createdPkgJson
    }

    static Map<String, Object> basicUPMPackageJson(String packageName, Map<String, Object> overrides = [:]) {
        Map<String, Object> base = [
                name: packageName,
                version: "0.0.0"
        ]
        base.putAll(overrides)
        return base
    }

}

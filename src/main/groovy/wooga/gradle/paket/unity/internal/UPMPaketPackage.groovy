package wooga.gradle.paket.unity.internal

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

    File generateBasicPackageDotJson(String packageName) {
        return createBasicPackageJson(baseDir, packageName)
    }

    static File createBasicPackageJson(File baseDir, String packageName) {
        def createdPkgJson = new File(baseDir, "package.json")
        createdPkgJson << UPMPaketPackage.classLoader.getResource("package.json.template")
                .text.replaceAll("%%UPM_PACKAGE_NAME%%", packageName)
        return createdPkgJson;
    }

}

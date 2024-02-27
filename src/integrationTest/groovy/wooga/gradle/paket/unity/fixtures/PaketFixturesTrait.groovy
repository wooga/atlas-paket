package wooga.gradle.paket.unity.fixtures

import java.nio.file.Files
import java.util.stream.Collectors

trait PaketFixturesTrait {

    abstract File getProjectDir()

    File fakePaketPackage(String name,
                          File baseDir = projectDir,
                          File installedPackagesDir = new File(projectDir, "packages"),
                          File unityDir = new File(projectDir, "unity")) {
        def dependencies = new File(projectDir, "paket.dependencies")
        dependencies << """
        source https://nuget.org/api/v2
        nuget ${name}
          """.stripIndent()

        def lockFile = new File(projectDir, "paket.lock")
        lockFile << """${name}""".stripIndent().trim()

        def referencesFile = new File(unityDir, "paket.unity3d.references")
        referencesFile.parentFile.mkdirs()
        referencesFile << "\n${name}".stripIndent().trim()

        def packageFolder = new File(installedPackagesDir, "$name")
        packageFolder.mkdirs()
        packageFolder.with {
            new File(it, "content/innerFolder").mkdirs()
            new File(it, "content/innerFolder/something.cs") << "CONTENT"
        }
        return packageFolder
    }

    Tuple2<File, File> fakeUPMPaketPackage(String name,
                                           File unityDir = new File(projectDir, "unity"),
                                           File installedPackagesDir = new File(projectDir, "packages"),
                                           File baseDir = projectDir) {
        def packageFolder = fakePaketPackage(name, baseDir, installedPackagesDir, unityDir)
        createMetafiles(packageFolder)
        new File(packageFolder, "content/innerFolder/package.json") << """{"name" : "com.something.${name.toLowerCase()}", "displayName": "${name}"}"""
        new File(packageFolder, "content/innerFolder/package.json.meta") << "META"
        return [packageFolder, new File(packageFolder, "content/innerFolder/package.json")]
    }

    List<File> createMetafiles(File baseFolder) {
        def metaCandidates = Files.walk(baseFolder.toPath()).map {it.toFile() }
            .filter { it != baseFolder }
            .filter {!it.name.endsWith(".meta") }

        def createdMetas = metaCandidates.map { new File(it.parentFile, "${it.name}.meta") }
            .filter {!it.exists() }
            .map {it << "META" }
            .collect(Collectors.toList())

        return createdMetas
    }
}

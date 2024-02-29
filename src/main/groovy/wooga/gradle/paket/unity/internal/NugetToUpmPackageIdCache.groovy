package wooga.gradle.paket.unity.internal


import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty

import java.nio.file.Files
import java.nio.file.Path

/**
 * Contains a cache of identifiers of Unity packages
 * that are generated from nuget then converted into UPM.
 */
class NugetToUpmPackageIdCache {

    static final String packageManifestFileName = "package.json"
    static final String namePropertyKey = "name"
    static final String displayNamePropertyKey = "displayName"

    final Project project
    final File paketPackagesDirectory
    final FileCollection inputFiles
    final File outputDirectory
    final MapProperty<String, Map<String, Object>> paketUpmPackageManifests

    Map<String, String> nugetToUPMPackageIdCache = [:] as Map<String, String>
    final String defaultNamespace

    NugetToUpmPackageIdCache(Project project,
                             File paketPackagesDirectory,
                             FileCollection inputs,
                             File outputDirectory,
                             MapProperty<String, Map<String, Object>> packageManifests,
                             String namespace = "com.wooga.nuget") {
        this.project = project
        this.paketPackagesDirectory = paketPackagesDirectory
        this.inputFiles = inputs
        this.outputDirectory = outputDirectory
        this.paketUpmPackageManifests = packageManifests
        this.defaultNamespace = namespace

        populateCacheFromInputFiles()
        populateCacheFromOutputDirectory()
    }

    String getUpmId(String nugetId) {
        nugetToUPMPackageIdCache[nugetId] ?: generateUpmId(nugetId)
    }

    boolean containsKey(String nugetId) {
        nugetToUPMPackageIdCache.containsKey(nugetId)
    }

    void dumpCacheToLog() {
        nugetToUPMPackageIdCache.each { project.logger.info("nugetToUPMPackageIdCache[${it.key}]=${it.value}") }
    }

    private String generateUpmId(String paketId) {
        def manifestOverrides = paketUpmPackageManifests.getting(paketId)?.getOrElse([:])
        manifestOverrides[namePropertyKey] ?: "${defaultNamespace}.${paketId.toLowerCase()}"
    }

    private Path getAbsolutePath(String directory) {
        return project.file(directory).toPath().toAbsolutePath()
    }

    private Map<String, Path> findPackageJsons(Path dirPath) {
        Map<String, Path> map = [:]
        inputFiles.each {
            if (it.name == packageManifestFileName) {
                def relativePath = dirPath.relativize(getAbsolutePath(it.path))
                def paketId = relativePath[0].toString()
                if (isNewOrCloserJson(relativePath, map[paketId])) map[paketId] = relativePath
            }
        }
        return map
    }

    private static boolean isNewOrCloserJson(Path newPath, Path existingPath) {
        !existingPath || existingPath.iterator().size() > newPath.iterator().size()
    }

    private void populateCacheFromInputFiles() {

        def paketPackagesDirPath = paketPackagesDirectory.toPath()
        def packageManifestMap = findPackageJsons(paketPackagesDirPath)

        inputFiles.each {
            def filePath = getAbsolutePath(it.path)
            def relativeFilePath = paketPackagesDirPath.relativize(filePath)
            def paketId = relativeFilePath[0].toString()
            if (!packageManifestMap.containsKey(paketId)) {
                def upmName = generateUpmId(paketId)
                nugetToUPMPackageIdCache[paketId] = upmName
            }
        }
        packageManifestMap.each { paketId, packagePath ->
            updateCacheFromPackageJson(paketPackagesDirPath.resolve(packagePath), paketId)
        }
    }

    private void updateCacheFromPackageJson(Path packagePath, String paketId) {
        if (Files.exists(packagePath)) {
            try {
                def packageManifest = new JsonSlurper().parse(packagePath)
                if (packageManifest.containsKey(namePropertyKey)) nugetToUPMPackageIdCache[paketId] = packageManifest[namePropertyKey]
            } catch (Exception e) {
                project.logger.error("Failed to parse ${packageManifestFileName}", e)
            }
        }
    }

    private void populateCacheFromOutputDirectory() {
        def outputPackageManifests = findOutputPackageJsons()
        outputPackageManifests.each {
            def packageManifestMap = new JsonSlurper().parse(it.value)
            if (packageManifestMap.containsKey(namePropertyKey) && packageManifestMap.containsKey(displayNamePropertyKey)) {
                def paketId = packageManifestMap[displayNamePropertyKey]
                if (!nugetToUPMPackageIdCache.containsKey(paketId)) nugetToUPMPackageIdCache[paketId] = packageManifestMap[namePropertyKey]
            }
        }
    }

    private LinkedHashMap<String, File> findOutputPackageJsons() {
        LinkedHashMap<String, File> map = [:]
        project.fileTree(outputDirectory).visit {
            if (it.name == packageManifestFileName) map[it.relativePath.segments[0]] = it.file
        }
        return map
    }

    // TODO: Parse the version? Though it's not needed since these end up getting consumed as "Custom" packages by Unity
    Map<String, Object> generateManifest(String nugetId, Map<String, Object> overrides = [:]) {

        def upmmId = getUpmId(nugetId)

        Map<String, Object> base = [
            name       : upmmId,
            displayName: nugetId,
            version    : "0.0.0"
        ]
        base.putAll(overrides)
        return base
    }

}

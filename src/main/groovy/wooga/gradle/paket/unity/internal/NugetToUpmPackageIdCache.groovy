package wooga.gradle.paket.unity.internal

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import wooga.gradle.paket.base.utils.internal.PaketUPMWrapperReference

import java.nio.file.Files
import java.nio.file.Path

/**
 * Contains a cache of identifiers of Unity packages
 * that are generated from nuget then converted into UPM.
 */
class NugetToUpmPackageIdCache {

    static final String PACKAGE_JSON = "package.json"
    static final String NAME = "name"
    static final String DISPLAY_NAME = "displayName"

    final Project project
    final FileCollection inputFiles
    final File outputDirectory
    final MapProperty<String, Map<String, Object>> paketUpmPackageManifests

    Map<String, String> nugetToUPMPackageIdCache = [:] as Map<String, String>
    String defaultNamespace = "com.wooga.nuget"

    NugetToUpmPackageIdCache(Project project,
                             FileCollection inputs,
                             File outputDirectory,
                             MapProperty<String, Map<String, Object>> packageManifests,
                             String namespace) {
        this.project = project
        this.inputFiles = inputs
        this.outputDirectory = outputDirectory
        this.paketUpmPackageManifests = packageManifests
        this.defaultNamespace ?= namespace

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
        manifestOverrides[NAME] ?: "${defaultNamespace}.${paketId.toLowerCase()}"
    }

    private Path getAbsolutePath(String directory) {
        project.file(directory).toPath().toAbsolutePath().normalize()
    }

    private Map<String, Path> findPackageJsons(Path dirPath) {
        Map<String, Path> map = [:]
        inputFiles.each {
            if (it.name == PACKAGE_JSON) {
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
        def packagesDirPath = getAbsolutePath(PaketUPMWrapperReference.getPackagesDirectory(project))
        def packageJsonMap = findPackageJsons(packagesDirPath)

        inputFiles.each {
            def relativePath = packagesDirPath.relativize(getAbsolutePath(it.path))
            def paketId = relativePath[0].toString()
            if (!packageJsonMap.containsKey(paketId)) {
                def upmName = generateUpmId(paketId)
                nugetToUPMPackageIdCache[paketId] = upmName
            }
        }
        packageJsonMap.each { paketId, packagePath ->
            updateCacheFromPackageJson(packagesDirPath.resolve(packagePath), paketId)
        }
    }

    private void updateCacheFromPackageJson(Path packagePath, String paketId) {
        if (Files.exists(packagePath)) {
            try {
                def pkgJsonMap = new JsonSlurper().parse(packagePath)
                if (pkgJsonMap.containsKey(NAME)) nugetToUPMPackageIdCache[paketId] = pkgJsonMap[NAME]
            } catch (Exception e) {
                project.logger.error("Failed to parse ${PACKAGE_JSON}", e)
            }
        }
    }

    private void populateCacheFromOutputDirectory() {
        def outputPackageJsons = findOutputPackageJsons()
        outputPackageJsons.each {
            def pkgJsonMap = new JsonSlurper().parse(it.value)
            if (pkgJsonMap.containsKey(NAME) && pkgJsonMap.containsKey(DISPLAY_NAME)) {
                def paketId = pkgJsonMap[DISPLAY_NAME]
                if (!nugetToUPMPackageIdCache.containsKey(paketId)) nugetToUPMPackageIdCache[paketId] = pkgJsonMap[NAME]
            }
        }
    }

    private LinkedHashMap<String, File> findOutputPackageJsons() {
        LinkedHashMap<String, File> map = [:]
        project.fileTree(outputDirectory).visit {
            if (it.name == PACKAGE_JSON) map[it.relativePath.segments[0]] = it.file
        }
        return map
    }

    // TODO: Parse the version?
     Map<String, Object> generateManifest(String nugetId, Map<String, Object> overrides = [:]) {

        def upmmId = getUpmId(nugetId)

        Map<String, Object> base = [
            name: upmmId,
            displayName: nugetId,
            version: "0.0.0"
        ]
        base.putAll(overrides)
        return base
    }

}

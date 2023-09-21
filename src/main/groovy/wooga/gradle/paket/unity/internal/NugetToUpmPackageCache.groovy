package wooga.gradle.paket.unity.internal

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.provider.MapProperty
import wooga.gradle.paket.base.utils.internal.PaketUPMWrapperReference

import java.nio.file.Files
import java.nio.file.Path

class NugetToUpmPackageCache
{
    final Project project
    final FileCollection inputFiles
    final File outputDirectory
    final MapProperty<String, Map<String, Object>> paketUpmPackageManifests

    Map<String, String> nugetToUPMPackageIdCache = [:] as Map<String,String>

    NugetToUpmPackageCache(Project project, FileCollection inputs, File outputDirectory, MapProperty<String, Map<String, Object>> packageManifests) {
        this.project = project
        this.inputFiles = inputs
        this.outputDirectory = outputDirectory
        this.paketUpmPackageManifests = packageManifests

        populateCacheFromInputFiles()
        populateCacheFromOutputDirectory()
    }

    public String getUpmId(String nugetId)
    {
        nugetToUPMPackageIdCache.containsKey(nugetId) ? nugetToUPMPackageIdCache[nugetId] : generateUpmId(nugetId)
    }

    protected String generateUpmId(String paketId)
    {
        def pkgJsonOverrides = paketUpmPackageManifests.getting(paketId).getOrElse([:])
        return pkgJsonOverrides.containsKey("name") ? pkgJsonOverrides["name"] : "com.wooga.nuget.${paketId.toLowerCase()}"
    }

    public containsKey(String nugetId)
    {
        nugetToUPMPackageIdCache.containsKey(nugetId)
    }

    public void dumpCacheTolog(Logger logger) {
        nugetToUPMPackageIdCache.each { logger.info("nugetToUPMPackageIdCache[${it.key}]=${it.value}") }
    }

    protected Path getAbsolutePath(String directory) {
        return project.file(directory).toPath().toAbsolutePath().normalize()
    }

    protected Map<String, Path> findPackageJsons(Path dirPath) {
        def map = [:]
        inputFiles.each {
            if (it.name == "package.json") {
                def relativePath = dirPath.relativize(getAbsolutePath(it.path))
                def paketId = relativePath[0].toString()
                if (isNewOrCloserJson(relativePath, map[paketId])) map[paketId] = relativePath
            }
        }
        return map
    }

    protected static boolean isNewOrCloserJson(Path newPath, Path existingPath) {
        return !existingPath || existingPath.iterator().size() > newPath.iterator().size()
    }

    protected void populateCacheFromInputFiles() {
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

    protected void updateCacheFromPackageJson(Path packagePath, String paketId) {
        if (Files.exists(packagePath)) {
            def pkgJsonMap = new JsonSlurper().parse(packagePath)
            if (pkgJsonMap.containsKey("name")) nugetToUPMPackageIdCache[paketId] = pkgJsonMap["name"]
        }
    }
    protected void populateCacheFromOutputDirectory() {
        def outputPackageJsons = findOutputPackageJsons()
        outputPackageJsons.each {
            def pkgJsonMap = new JsonSlurper().parse(it.value)
            if (pkgJsonMap.containsKey("name") && pkgJsonMap.containsKey("displayName")) {
                def paketId = pkgJsonMap["displayName"]
                if (!nugetToUPMPackageIdCache.containsKey(paketId)) nugetToUPMPackageIdCache[paketId] = pkgJsonMap["name"]
            }
        }
    }

    protected LinkedHashMap<String, File> findOutputPackageJsons() {
        def map = [:]
        project.fileTree(outputDirectory).visit {
            if (it.name == "package.json") map[it.relativePath.segments[0]] = it.file
        }
        return map
    }
}

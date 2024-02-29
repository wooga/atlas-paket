package wooga.gradle.paket.unity.tasks

import groovy.transform.Internal
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import wooga.gradle.paket.base.utils.internal.PaketLock
import wooga.gradle.paket.base.utils.internal.PaketUPMWrapperReference
import wooga.gradle.paket.base.utils.internal.PaketUnityReferences

import java.util.function.Function

/**
 * A task that installs packages onto an Unity Project
 */
abstract class PaketUnityInstallTask extends ConventionTask {

    /**
     * @return the path to a {@code paket.unity3d.references} file
     */
    @InputFile
    File referencesFile

    /**
     * @return the path to a {@code paket.lock} file
     */
    @InputFile
    File lockFile

    /**
     * @return The directory where Unity packages should be located
     */
    @OutputDirectory
    @Internal
    File getOutputDirectory() {
        new File(getReferencesFile().getParentFile(), "Packages")
    }

    /**
     * @return The path to where paket originally installs (extracts) the downloaded packages
     */
    @org.gradle.api.tasks.Internal
    File getPaketPackagesDirectory() {
        // The MacOS Java implementation resolves 'Packages' and 'packages' in separate, as if it was on a unix system.
        // However, MacOS actually treat those two folders as the same, so we use .canonicalPath to make sure that we get the
        // correct capitalization on the existing folder.
        def normalizedPath = new File(project.projectDir, "packages").canonicalPath
        return new File(normalizedPath)
    }

    /**
     * Each UPM package has a manifest file with metadata about the package's contents
     */
    static final String packageManifestFileName = "package.json"

    /**
     * The extension used by Unity's assembly definition files
     */
    final static String assemblyDefinitionFileExtension = "asmdef"

    /**
     * @param packageName The name of the package
     * @return True if the
     */
    boolean isUPMWrapper(String packageName) {
        return (new PaketUPMWrapperReference(paketPackagesDirectory, packageName)).exists
    }

    /**
     * @return The input files to use for the task based on the referenced nuget packages
     */
    protected FileCollection collectInputFiles(Function<String, Set<String>> getFiles) {
        Set<File> files = []

        if (!lockFile.exists()) {
            return null
        }

        def locks = new PaketLock(lockFile)
        def references = new PaketUnityReferences(referencesFile)
        def dependencies = locks.getAllDependencies(references.nugets)
        dependencies.each { nugetId ->
            files << getFiles(nugetId)
        }

        project.files(files)
    }
}

/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.unity.tasks


import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import wooga.gradle.paket.base.utils.internal.*
import wooga.gradle.paket.unity.PaketUnityPlugin

/**
 * A task to unwrap UPM packages from within Paket Wrapper Packages into Unity3D projects as local UPM Overrides.
 * <p>
 * This task will take as input a references and lock file to compute the nuget packages that contain upm references.
 * Then it will either download or just unpack the referenced tars from them into the unity project's {@code Packages} folder.
 * This will effectively instruct Unity to locally override those UPM package, and force it to use this version instead of whatever is defined in the Unity UPM manifest
 * <p>
 * Example:
 * <pre>
 * {@code
 *     task unwrapUpmPackages(type:wooga.gradle.paket.unity.tasks.PaketUnwrapUPMPackages) {*         referencesFile = file('paket.unity3D.references')
 *         lockFile = file('../paket.lock')
 *}*}
 * </pre>
 */
class PaketUnwrapUPMPackages extends PaketUnityInstallTask {

    /**
     * @return the paket.upm.wrapper.reference's of the wrapper packages to unwrap into the Unity3D/Packages project.
     */
    @InputFiles
    FileCollection getInputFiles() {
        collectInputFiles(nugetId -> {
            if (isUPMWrapper(nugetId)) {
                return [new PaketUPMWrapperReference(paketPackagesDirectory, nugetId).file] as Set<File>
            }
        })
    }

    PaketUnwrapUPMPackages() {
        description = 'Unwraps wrapped UPM into paket dependencies into unity projects as local UPM Overrides'
        group = PaketUnityPlugin.GROUP
    }

    @TaskAction
    protected performCopy(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            cleanLocalUpmOverrides()
        }

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails outOfDate) {
                if (inputFiles.contains(outOfDate.file) && PaketUPMWrapperReference.IsReferenceFile(outOfDate.file)) {
                    def upmWrapperRef = new PaketUPMWrapperReference(outOfDate.file)
                    if (upmWrapperRef.exists) {
                        cleanLocalUpmOverride(upmWrapperRef)

                        upmWrapperRef.upmPackageURL.startsWith("http")
                            ? new DownloadAndUnpackTar(upmWrapperRef, getOutputDirectory()).exec(project)
                            : new UnwrapUpm(upmWrapperRef, getOutputDirectory()).exec(project)
                    }
                }
            }
        })

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails removed) {
                if (PaketUPMWrapperReference.IsReferenceFile(removed.file)) {
                    logger.info("remove: ${removed.file}")
                    def upmWrapper = new PaketUPMWrapperReference(removed.file);
                    if (upmWrapper.exists) {
                        cleanLocalUpmOverride(new PaketUPMWrapperReference(removed.file))
                    }
                    removed.file.delete()
                    assert removed.removed
                }
            }
        })
    }


    private PaketUPMWrapperReference[] getWrappedUPMPackages() {
        def references = new PaketUnityReferences(referencesFile)

        if (!getLockFile().exists()) {
            return null
        }

        Set<PaketUPMWrapperReference> packages = []
        def locks = new PaketLock(getLockFile())
        def dependencies = locks.getAllDependencies(references.nugets)
        dependencies.each { nugetId ->
            def upmPackage = new PaketUPMWrapperReference(paketPackagesDirectory, nugetId)
            if (upmPackage.exists) {
                packages << upmPackage
            }
        }

        return packages
    }

    protected void cleanLocalUpmOverrides() {
        getWrappedUPMPackages().each { upmWrapperReference ->
            cleanLocalUpmOverride(upmWrapperReference);
        }
    }

    protected void cleanLocalUpmOverride(PaketUPMWrapperReference upmWrapperReference) {
        // strip version, since we want to delete old versions
        String packageName = upmWrapperReference.upmPackageNameUnversioned

        def matchingPackageOverrideDirs = []

        project.fileTree(getOutputDirectory()).visit(new FileVisitor() {
            @Override
            void visitDir(FileVisitDetails dirDetails) {
                if (dirDetails.file.name.startsWith(packageName)) {
                    matchingPackageOverrideDirs << dirDetails.file;
                }
            }

            @Override
            void visitFile(FileVisitDetails fileDetails) {
            }
        })

        matchingPackageOverrideDirs.reverseEach { it.deleteDir() }
    }
}

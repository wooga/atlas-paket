package wooga.gradle.paket.base.utils.internal

import org.gradle.api.Project

/*
 * Copyright 2022 Wooga GmbH
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

class PaketUPMWrapperReference {
    public String upmPackageNameUnversioned
    public String upmPackageName
    public String upmPackageURL;
    public File upmPackageTar
    public File file;
    public boolean exists

    public final static String upmWrapperReferenceFile = "paket.upm.wrapper.reference"

    public static boolean IsReferenceFile(File file)
    {
        return file.name.equals(upmWrapperReferenceFile)
    }

    PaketUPMWrapperReference(File wrapperReferencesFile) {
        file = wrapperReferencesFile
        exists = false
        if (file.exists()) {
            exists = true
            def parts = file.text.trim().split(";")
            upmPackageURL = parts[0]
            upmPackageName  = parts[1]
            upmPackageNameUnversioned = upmPackageName.split("@")[0]
            upmPackageTar = new File ("${file.parentFile.path}/${upmPackageURL}")
        }
    }

    PaketUPMWrapperReference(String nugetPackage, Project project) {
        this(project.file ("packages/${nugetPackage}/lib/${upmWrapperReferenceFile}"))
    }

}


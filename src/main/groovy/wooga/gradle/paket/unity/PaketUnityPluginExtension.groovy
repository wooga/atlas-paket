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

package wooga.gradle.paket.unity

import org.gradle.api.file.FileCollection
import wooga.gradle.paket.base.PaketPluginExtension

/**
 * A extension point for paket unity
 */
interface PaketUnityPluginExtension extends PaketPluginExtension {

    /**
     * Returns a {@link FileCollection} object containing all {@code paket.unity3D.references} files.
     *
     * @return a collection of all {@code paket.unity3D.references} files.
     */
    FileCollection getPaketReferencesFiles()

    /**
     * @return  the paket unity output directory name
     */
    String getPaketOutputDirectoryName()

    /**
     * Sets the paket unity output directory name
     * @param directory name of the output directory
     */
    void setPaketOutputDirectoryName(String directory)
}

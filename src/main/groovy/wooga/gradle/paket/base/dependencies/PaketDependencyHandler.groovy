/*
 * Copyright 2017 the original author or authors.
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

package wooga.gradle.paket.base.dependencies

import org.gradle.api.tasks.Nested

interface PaketDependencyHandler extends PaketDependencyConfigurationContainer, PaketDependencyMacros {

    static final String MAIN_GROUP = "main"

    /**
     * Returns the {@code main} dependency configuration.
     *
     * This dependency configuration acts as the default group <b>Main</b>
     * in the paket.dependencies file.
     *
     * @return the default {@code PaketDependencyConfiguration} object
     */
    PaketDependencyConfiguration getMain()

    /**
     * Executes the provided configuration closure on the @{code getMain} configuration
     *
     * @param depSpec a configuration closure executed on the main configuration
     */
    void main(Closure depSpec)

    @Nested
    PaketDependencyConfigurationContainer getConfigurationContainer()

    boolean hasDependencies()
}
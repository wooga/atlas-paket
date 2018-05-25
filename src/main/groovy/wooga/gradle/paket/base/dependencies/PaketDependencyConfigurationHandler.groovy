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

import org.gradle.api.Action

/**
 * A handler object to add and configure paket dependency configurations.
 */
interface PaketDependencyConfigurationHandler {

    /**
     * Adds a paket dependency.
     *
     * @param dependencyNotation a valid paket dependency notation.
     * @return {@code true} if dependency was added to the configuration
     */
    boolean add(final String dependencyNotation)

    /**
     * Adds a paket dependency and configures it with the given closure.
     *
     * @param dependencyNotation a valid paket dependency notation.
     * @param depSpec a {@code Closure} to configure the created {@link PaketDependency}
     * @return {@code true} if dependency was added to the configuration
     */
    boolean add(final String dependencyNotation, Closure depSpec)

    /**
     * Adds a paket dependency and configures it with the given action.
     *
     * @param dependencyNotation a valid paket dependency notation.
     * @param depSpec a {@code Action} to configure the created {@link PaketDependency}
     * @return {@code true} if dependency was added to the configuration
     */
    boolean add(final String dependencyNotation, Action<PaketDependency> depSpec)

    /**
     * Adds a {@link wooga.gradle.paket.base.dependencies.internal.DefaultNugetDependency}.
     *
     * @param dependencyNotation a valid nuget dependency notation.
     * @return {@code true} if dependency was added to the configuration
     */
    boolean nuget(final String dependencyNotation)

    /**
     * Adds a {@link wooga.gradle.paket.base.dependencies.internal.DefaultNugetDependency}.
     *
     * @param dependencyNotation a valid nuget dependency notation.
     * @param depSpec a {@code Closure} to configure the created {@link wooga.gradle.paket.base.dependencies.internal.DefaultNugetDependency}
     * @return {@code true} if dependency was added to the configuration
     */
    boolean nuget(String dependencyNotation, Closure depSpec)

    /**
     * Adds a {@link wooga.gradle.paket.base.dependencies.internal.DefaultNugetDependency}.
     *
     * @param dependencyNotation a valid nuget dependency notation.
     * @param depSpec a {@code Action} to configure the created {@link wooga.gradle.paket.base.dependencies.internal.DefaultNugetDependency}
     * @return {@code true} if dependency was added to the configuration
     */
    boolean nuget(String dependencyNotation, Action<PaketDependency> depSpec)
}
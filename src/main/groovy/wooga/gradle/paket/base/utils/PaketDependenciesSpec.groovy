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

package wooga.gradle.paket.base.utils

/**
 * Object representation of a {@code paket.dependencies} file.
 */
interface PaketDependenciesSpec {

    /**
     * Returns a set of sources configured in {@code paket.dependencies} file.
     *
     * @return  sources for paket to fetch dependencies from
     */
    Set<String> getSources()

    /**
     * Returns a set of {@code nuget} dependencies configured {@code paket.dependencies} file.
     *
     * @return all nuget dependencies in {@code paket.dependencies} file.
     */
    Set<String> getNugetDependencies()

    /**
     * Returns as set of configured .NET frameworks.
     * @return list of .NET frameworks configured
     */
    List<String> getFrameworks()

}

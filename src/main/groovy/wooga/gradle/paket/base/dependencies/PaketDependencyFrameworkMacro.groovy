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

package wooga.gradle.paket.base.dependencies

import org.gradle.api.tasks.Input

interface PaketDependencyFrameworkMacro {

    /**
     * Returns a list of restricted frameworks
     * @return
     */
    @Input
    List<String> getFrameworks()

    /**
     * Sets the list of restricted frameworks as @{Iterable<String>}
     * @param frameworks
     */
    void setFrameworks(Iterable<String> frameworks)

    /**
     * Adds one or more frameworks to the list of restricted frameworks
     * @param frameworks
     */
    void frameworks(String... frameworks)

    /**
     * Adds one or more frameworks to the list of restricted frameworks
     * @param frameworks
     */
    void frameworks(Iterable<String> frameworks)

    /**
     * Adds a single framework to the list of restricted frameworks
     * @param frameworks
     */
    void framework(String framework)
}

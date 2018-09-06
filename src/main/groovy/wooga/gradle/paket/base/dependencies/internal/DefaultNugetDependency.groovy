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

package wooga.gradle.paket.base.dependencies.internal

import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency
import wooga.gradle.paket.base.dependencies.NugetDependency
import wooga.gradle.paket.base.dependencies.PaketDependencyType

class DefaultNugetDependency extends AbstractDependency implements NugetDependency {

    private String name
    private String version

    DefaultNugetDependency(final String name, final String version) {
        this.name = name
        this.version = version
    }

    @Override
    String getGroup() {
        return null
    }

    @Override
    String getName() {
        return name
    }

    @Override
    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
    }

    DefaultNugetDependency version(String version) {
        this.setVersion(version)
        this
    }

    @Override
    boolean contentEquals(Dependency dependency) {
        (this.name == dependency.name && this.version == dependency.version)
    }

    @Override
    Dependency copy() {
        return new DefaultNugetDependency(name, version)
    }

    @Override
    String toString() {
        def s = [getType(), getName()]
        if(getVersion()) {
            s << getVersion()
        }
        return s.join(" ")
    }

    @Override
    PaketDependencyType getType() {
        return PaketDependencyType.nuget
    }
}

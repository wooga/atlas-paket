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

package wooga.gradle.paket.base.dependencies.internal

import org.gradle.internal.reflect.Instantiator
import wooga.gradle.paket.base.dependencies.NugetDependency
import wooga.gradle.paket.base.dependencies.PaketDependency
import wooga.gradle.paket.base.dependencies.PaketDependencyFormatException
import wooga.gradle.paket.base.dependencies.PaketDependencyType

class PaketDependencyFactory {

    private final Instantiator instantiator


    PaketDependencyFactory(Instantiator instantiator) {
        this.instantiator = instantiator
    }

    PaketDependency create(String dependencyNotation) {
        def parts = dependencyNotation.split(' ', 2)
        def type = parts[0]
        def definition = parts[1]
        PaketDependency dependency
        switch (type as PaketDependencyType) {
            case PaketDependencyType.nuget:
                dependency = createNugetDependency(definition)
                break
            default:
                throw new PaketDependencyFormatException("Unsupported dependencyNotation ${dependencyNotation}")
        }

        dependency
    }

    NugetDependency createNugetDependency(String dependencyNotation) {
        def parts = dependencyNotation.split(' ', 2)
        def name = parts[0]
        def version = (parts.size() == 2) ? parts[1] : null
        this.instantiator.newInstance(DefaultNugetDependency.class, name,version)
    }
}

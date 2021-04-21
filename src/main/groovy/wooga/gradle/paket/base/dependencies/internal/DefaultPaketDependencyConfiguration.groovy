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

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.Actions
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil
import wooga.gradle.paket.base.dependencies.PaketDependency
import wooga.gradle.paket.base.dependencies.PaketDependencyConfiguration
import wooga.gradle.paket.base.dependencies.PaketDependencyType

class DefaultPaketDependencyConfiguration extends AbstractNamedDomainObjectContainer<PaketDependency> implements PaketDependencyConfiguration, Named {

    private final String name
    private final PaketDependencyFactory dependencyFactory

    DefaultPaketDependencyConfiguration(final String name, Instantiator instantiator = null) {
        super(PaketDependency.class, instantiator ?: DirectInstantiator.INSTANCE, CollectionCallbackActionDecorator.NOOP)
        this.name = name
        this.dependencyFactory = instantiator.newInstance(PaketDependencyFactory, instantiator)
    }

    @Override
    String getName() {
        return name
    }

    @Override
    boolean add(String dependencyNotation) {
        add(dependencyNotation, Actions.doNothing())
    }

    @Override
    boolean add(String dependencyNotation, Closure depSpec) {
        add(dependencyNotation, ConfigureUtil.configureUsing(depSpec))
    }

    @Override
    boolean add(String dependencyNotation, Action<PaketDependency> depSpec) {
        create(dependencyNotation, depSpec)
    }

    @Override
    boolean nuget(String dependencyNotation) {
        nuget(dependencyNotation, Actions.doNothing())
    }

    @Override
    boolean nuget(String dependencyNotation, Closure depSpec) {
        nuget(dependencyNotation, ConfigureUtil.configureUsing(depSpec))
    }

    @Override
    boolean nuget(String dependencyNotation, Action<PaketDependency> depSpec) {
        add("${PaketDependencyType.nuget} ${dependencyNotation}", depSpec)
    }

    @Override
    protected PaketDependency doCreate(String dependencyNotation) {
        return dependencyFactory.create(dependencyNotation)
    }
}

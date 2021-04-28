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

import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.internal.reflect.Instantiator
import wooga.gradle.paket.base.dependencies.PaketDependencyConfiguration
import wooga.gradle.paket.base.dependencies.PaketDependencyConfigurationContainer

class DefaultPaketDependencyConfigurationContainer extends AbstractNamedDomainObjectContainer<PaketDependencyConfiguration> implements PaketDependencyConfigurationContainer {

    private final Instantiator instantiator

    DefaultPaketDependencyConfigurationContainer(final Instantiator instantiator) {
        super(PaketDependencyConfiguration.class, instantiator, CollectionCallbackActionDecorator.NOOP)
        this.instantiator = instantiator
    }

    @Override
    protected PaketDependencyConfiguration doCreate(String name) {
        instantiator.newInstance(DefaultPaketDependencyConfiguration.class, name, instantiator)
    }
}

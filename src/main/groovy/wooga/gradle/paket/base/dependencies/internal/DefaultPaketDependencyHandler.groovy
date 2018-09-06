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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.metaobject.DynamicInvokeResult
import org.gradle.internal.metaobject.MethodAccess
import org.gradle.internal.metaobject.MethodMixIn
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.util.CollectionUtils
import org.gradle.util.Configurable
import org.gradle.util.ConfigureUtil
import wooga.gradle.paket.base.dependencies.*

class DefaultPaketDependencyHandler implements PaketDependencyHandler, MethodMixIn {

    @Delegate(includeTypes = [PaketDependencyConfigurationHandler.class], interfaces = false)
    private final DefaultPaketDependencyConfiguration mainConfiguration

    @Delegate
    private final PaketDependencyMacros macros

    @Delegate(excludeTypes = [Configurable.class])
    private final PaketDependencyConfigurationContainer configurationContainer
    private final DynamicDependencyConfigurationMethods dynamicMethods
    private final ServiceRegistry services
    private final Instantiator instantiator

    DefaultPaketDependencyHandler(final Project project) {
        services = (project as ProjectInternal).getServices()
        instantiator = services.get(Instantiator.class)
        configurationContainer = instantiator.newInstance(DefaultPaketDependencyConfigurationContainer,instantiator)
        mainConfiguration = configurationContainer.create(MAIN_GROUP)
        macros = instantiator.newInstance(DefaultPaketDependencyMacros)
        dynamicMethods = new DynamicDependencyConfigurationMethods()

        addRule("valid group", { String name ->
            if(NameUtil.validateName(name)) {
                create(name)
            }
        })
    }

    @Override
    boolean hasDependencies() {
        configurationContainer.every { it.isEmpty() }
    }

    void clear() {
        configurationContainer.each { it.clear() }
    }

    @Override
    PaketDependencyConfiguration getMain() {
        return mainConfiguration
    }

    NamedDomainObjectContainer<PaketDependencyConfiguration> configure(Closure configureClosure) {
        return ConfigureUtil.configureSelf(configureClosure, this)
    }

    @Override
    void main(@DelegatesTo(PaketDependencyConfiguration) Closure depSpec) {
        this.mainConfiguration.configure(depSpec)
    }

    @Override
    PaketDependencyConfigurationContainer getConfigurationContainer() {
        return this.configurationContainer
    }

    @Override
    MethodAccess getAdditionalMethods() {
        return dynamicMethods
    }

    private class DynamicDependencyConfigurationMethods implements MethodAccess {
        @Override
        boolean hasMethod(String name, Object... arguments) {
            List<?> normalizedArgs = CollectionUtils.flattenCollections(arguments)
            return (normalizedArgs.size() == 1
                        && normalizedArgs.get(0) instanceof Closure
                        && configurationContainer.findByName(name.toLowerCase()) != null)
        }

        @Override
        DynamicInvokeResult tryInvokeMethod(String name, Object... arguments) {
            if(!NameUtil.validateName(name)) {
                return DynamicInvokeResult.notFound()
            }

            PaketDependencyConfiguration configuration = configurationContainer.maybeCreate(name.toLowerCase())
            List<?> normalizedArgs = CollectionUtils.flattenCollections(arguments)

            if (normalizedArgs.size() == 1
                    && normalizedArgs.get(0) instanceof Closure) {
                return DynamicInvokeResult.found(configuration.configure((Closure) normalizedArgs.get(0)))
            } else {
                return DynamicInvokeResult.notFound()
            }
        }


    }

    private static class NameUtil {
        static boolean validateName(String name) {
            if(!Character.isJavaIdentifierStart(name.charAt(0))) {
                return false
            }

            name.chars.every { Character.isJavaIdentifierPart(it) }
        }
    }
}



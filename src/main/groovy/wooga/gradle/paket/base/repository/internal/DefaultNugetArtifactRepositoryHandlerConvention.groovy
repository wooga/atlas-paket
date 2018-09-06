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

package wooga.gradle.paket.base.repository.internal

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.internal.artifacts.DefaultArtifactRepositoryContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.internal.authentication.DefaultAuthenticationContainer
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil
import wooga.gradle.paket.base.repository.NugetRepositoryHandlerConvention
import wooga.gradle.paket.base.repository.NugetArtifactRepository

class DefaultNugetArtifactRepositoryHandlerConvention implements NugetRepositoryHandlerConvention<NugetArtifactRepository> {

    private final DefaultArtifactRepositoryContainer handler
    private final Instantiator instantiator
    private final FileResolver fileResolver

    DefaultNugetArtifactRepositoryHandlerConvention(RepositoryHandler handler, FileResolver fileResolver, Instantiator instantiator) {
        this.handler = handler
        this.instantiator = instantiator
        this.fileResolver = fileResolver
    }

    @Override
    NugetArtifactRepository nuget() {
        handler.addRepository(createNugetRepository(), NUGET_REPO_DEFAULT_NAME)
    }

    @Override
    NugetArtifactRepository nuget(Action<? super NugetArtifactRepository> action) {
        handler.addRepository(createNugetRepository(), NUGET_REPO_DEFAULT_NAME, action)
    }

    @Override
    NugetArtifactRepository nuget(Closure configureClosure) {
        return nuget(ConfigureUtil.configureUsing(configureClosure))
    }

    protected NugetArtifactRepository createNugetRepository() {
        instantiator.newInstance(DefaultNugetArtifactRepository.class, fileResolver, instantiator, createAuthenticationContainer())
    }

    protected AuthenticationContainer createAuthenticationContainer() {
        DefaultAuthenticationContainer container = instantiator.newInstance(DefaultAuthenticationContainer.class, instantiator)

        container.registerBinding(BasicAuthentication.class, DefaultBasicAuthentication.class)
        return container
    }
}

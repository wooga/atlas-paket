/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paketPublish

import org.gradle.api.Action
import org.gradle.api.internal.artifacts.DefaultArtifactRepositoryContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil

class DefaultNugetRepositoryHandler extends DefaultArtifactRepositoryContainer implements NugetRepositoryHandler {

    static String NUGET_REPO_DEFAULT_NAME = "nuget"

    DefaultNugetRepositoryHandler(Instantiator instantiator) {
        super(instantiator)
    }

    NugetArtifactRepository createRepo() {
        return new NugetRepository()
    }

    @Override
    NugetArtifactRepository nuget() {
        return addRepository(new NugetRepository(), NUGET_REPO_DEFAULT_NAME)
    }

    @Override
    NugetArtifactRepository nuget(Action<? super NugetArtifactRepository> action) {
        return addRepository(createRepo(), NUGET_REPO_DEFAULT_NAME, action)
    }

    @Override
    NugetArtifactRepository nuget(Closure configureClosure) {
        return nuget(ConfigureUtil.configureUsing(configureClosure))
    }
}

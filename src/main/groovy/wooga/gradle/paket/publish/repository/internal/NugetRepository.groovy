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

package wooga.gradle.paket.publish.repository.internal

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import wooga.gradle.paket.publish.repository.NugetArtifactRepository

class NugetRepository implements NugetArtifactRepository {

    String apiKey
    String url
    String endpoint
    String name
    String path

    def name(String name) {
        setName(name)
    }

    def url(String url) {
        if(path != null)
        {
            throw new Exception("path already set")
        }
        setUrl(url)
    }

    def apiKey(String apiKey) {
        setApiKey(apiKey)
    }

    def endpoint(String endpoint) {
        setEndpoint(endpoint)
    }

    def path(String path){
        if(url != null)
        {
            throw new Exception("url already set")
        }
        setPath(path)
    }

    def getDestination(){
        path ? path : url
    }

    @Override
    void content(Action<? super RepositoryContentDescriptor> action) {

    }
}

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

package wooga.gradle.paket.publish.repository

import org.gradle.api.artifacts.repositories.ArtifactRepository

interface NugetArtifactRepository extends ArtifactRepository {

    String getApiKey()
    void setApiKey(String apiKey)

    String getUrl()
    void setUrl(String url)

    String getEndpoint()
    void setEndpoint(String endpoint)
}

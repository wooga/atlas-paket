/*
 * Copyright 2017 Wooga GmbH
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

package wooga.gradle.paket.publish.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.concurrent.Callable

class DefaultPaketPushPluginExtension {

    static Logger logger = Logging.getLogger(DefaultPaketPushPluginExtension)

    static String NAME = "paketPublish"

    static String PUBLISH_URL_PROPERTY = "paket.publish.url"
    static String PUBLISH_API_KEY_PROPERTY = "paket.publish.apiKey"
    static String PUBLISH_REPOSITORY_NAME_PROPERTY = "paket.publish.repository"

    def publishRepositoryName = "nuget"

    String getPublishRepositoryName() {
        if(project.hasProperty(PUBLISH_REPOSITORY_NAME_PROPERTY))
        {
            logger.debug("has property `{}` {}", PUBLISH_REPOSITORY_NAME_PROPERTY, project.property(PUBLISH_REPOSITORY_NAME_PROPERTY))
            project.property(PUBLISH_REPOSITORY_NAME_PROPERTY)
        }
        else if (publishRepositoryName == null) {
            null
        }
        else if (publishRepositoryName instanceof Callable) {
            publishRepositoryName.call()
        } else {
            publishRepositoryName.toString()
        }
    }

    void publishRepositoryName(value) {
        this.publishRepositoryName = value
    }




    def defaultApiKey
    def defaultPublishUrl

    Project project

    DefaultPaketPushPluginExtension(Project project) {
        this.project = project
    }

    String getDefaultApiKey() {
        if(project.hasProperty(PUBLISH_API_KEY_PROPERTY))
        {
            logger.debug("has property `{}` {}", PUBLISH_API_KEY_PROPERTY, project.property(PUBLISH_API_KEY_PROPERTY))
            project.property(PUBLISH_API_KEY_PROPERTY)
        }
        else if (defaultApiKey == null) {
            null
        }
        else if (defaultApiKey instanceof Callable) {
            defaultApiKey.call()
        } else {
            defaultApiKey.toString()
        }
    }

    String getDefaultPublishUrl() {
        if(project.hasProperty(PUBLISH_URL_PROPERTY))
        {
            logger.debug("has property `{}` {}", PUBLISH_URL_PROPERTY, project.property(PUBLISH_URL_PROPERTY))
            project.property(PUBLISH_URL_PROPERTY)
        }
        else if (defaultPublishUrl == null) {
            null
        }
        else if (defaultPublishUrl instanceof Callable) {
            defaultPublishUrl.call()
        } else {
            defaultPublishUrl.toString()
        }
    }
}

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

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.concurrent.Callable

class PaketPushPluginExtension {

    static Logger logger = Logging.getLogger(PaketPushPluginExtension)

    def apiKey
    def publishURL

    Project project

    PaketPushPluginExtension(Project project) {
        this.project = project
    }

    String getApiKey() {
        if(project.hasProperty("paket.apiKey"))
        {
            logger.debug("has property `paket.apiKey` {}", project.property("paket.apiKey"))
            project.property("paket.apiKey")
        }
        else if (apiKey == null) {
            null
        }
        else if (apiKey instanceof Callable) {
            apiKey.call()
        } else {
            apiKey.toString()
        }
    }

    String getPublishURL() {
        if(project.hasProperty("paket.publishURL"))
        {
            logger.debug("has property `paket.publishURL` {}", project.property("paket.publishURL"))
            project.property("paket.publishURL")
        }
        else if (publishURL == null) {
            null
        }
        else if (publishURL instanceof Callable) {
            publishURL.call()
        } else {
            publishURL.toString()
        }
    }
}

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

package wooga.gradle.paket.publish.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask

import java.util.concurrent.Callable

/**
 * A task to invoke {@code paket push} command with given {@code .nupgk} file.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     task push(type:wooga.gradle.paket.publish.tasks.PaketPush) {
 *         url = "https://www.nuget.com"
 *         apiKey = "NUGET_API_KEY"
 *         inputFile = file('path/to/package.nupkg')
 *     }
 * }
 * </pre>
 */
class PaketPush extends AbstractPaketTask {

    static String COMMAND = "push"

    def url

    /**
     * Returns the URL of the NuGet feed.
     *
     * @return  Nuget feed URL
     */
    @Input
    String getUrl() {
        if( url == null)
        {
            null
        }
        else if (url instanceof Callable) {
            url.call()
        } else {
            url.toString()
        }
    }

    def apiKey

    /**
     * Returns the API key for the feed URL.
     *
     * @return  API key for the feed URL
     * @see     #getUrl()
     */
    @Optional
    @Input
    String getApiKey() {
        if( apiKey == null)
        {
            null
        }
        else if (apiKey instanceof Callable) {
            apiKey.call()
        } else {
            apiKey.toString()
        }
    }

    def inputFile

    /**
     * Returns the path to the .nupkg file
     *
     * @return  path to the .nupkg file
     */
    @InputFile
    File getinputFile() {
        project.file inputFile
    }

    def endpoint

    @Optional
    @Input
    String getEndpoint() {
        if( endpoint == null)
        {
            null
        }
        else if (endpoint instanceof Callable) {
            endpoint.call()
        } else {
            endpoint.toString()
        }
    }

    PaketPush() {
        super(PaketPush.class)
        description = "Pushes the given .nupkg file."
        paketCommand = COMMAND
    }

    @Override
    protected void configureArguments() {
        super.configureArguments()

        args << "url" << getUrl()

        if(getApiKey() != null)
        {
            args << "apikey" << getApiKey()
        }

        if(getEndpoint() != null)
        {
            args << "endpoint" << getEndpoint()
        }

        args << "file" << getinputFile().path
    }
}

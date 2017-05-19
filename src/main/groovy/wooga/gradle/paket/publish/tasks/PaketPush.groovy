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

package wooga.gradle.paket.publish.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import wooga.gradle.paket.base.tasks.PaketTask

import java.util.concurrent.Callable

class PaketPush extends PaketTask {

    @Input
    def url

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

    @Optional
    @Input
    def apiKey

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

    @InputFile
    def inputFile

    File getinputFile() {
        project.file inputFile
    }

    PaketPush() {
        super()
        description = "Pushes the given .nupkg file."
    }

    @TaskAction
    void performRestore() {
        performPaketCommand { cmd ->
            cmd << "push"
            cmd << "url" << getUrl()

            if(getApiKey() != null)
            {
                cmd << "apikey" << getApiKey()
            }

            cmd << "file" << getinputFile().path
        }
    }
}

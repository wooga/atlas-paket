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

package net.wooga.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input

class PaketTask extends DefaultTask {

    @Input
    protected String runtime

    PaketTask() {
        super()
        group = 'Paket'
    }

    @Override
    Task configure(Closure closure) {
        String osName = System.getProperty("os.name").toLowerCase();
        runtime = (osName.contains("windows")) ? "" : "mono "
        return super.configure(closure)
    }

    void checkPaket() {
        def paketUnityCmd = new File('.paket/paket.unity3d.exe')
        if (! paketUnityCmd.exists()) {
            println("Install paket.unity3d.exe")
            println "${runtime}.paket/paket.unity3d.bootstrapper.exe".execute().text
        }
    }
}

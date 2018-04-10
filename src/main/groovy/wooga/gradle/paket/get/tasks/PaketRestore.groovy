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

package wooga.gradle.paket.get.tasks

import org.gradle.api.specs.Spec
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask
import wooga.gradle.paket.internal.PaketCommand

/**
 * A task to invoke {@code paket restore} command.
 */
class PaketRestore extends AbstractPaketTask {

    @InputFile
    File getPaketLock() {
        project.file("paket.lock")
    }

    @OutputFile
    File getRestoreCacheOut() {
        project.file("paket-files/paket.restore.cached")
    }

    PaketRestore() {
        super(PaketRestore.class)
        description = "Download the dependencies specified by the paket.lock file into the packages/ directory."
        paketCommand = PaketCommand.RESTORE
        outputs.upToDateWhen(new Spec<PaketRestore>() {
            @Override
            boolean isSatisfiedBy(PaketRestore restore) {
                if(!restore.getRestoreCacheOut().exists() || !restore.getPaketLock().exists()) {
                    return false
                }

                restore.getPaketLock().text == restore.getRestoreCacheOut().text
            }
        })
    }
}

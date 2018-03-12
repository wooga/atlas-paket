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

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import wooga.gradle.paket.internal.PaketCommand
import wooga.gradle.paket.base.tasks.internal.AbstractPaketTask

/**
 * A task to invoke {@code paket update} command.
 */
class PaketUpdate extends AbstractPaketTask {

    /**
     * Returns an optional nuget package id to update
     *
     * @returns  a package id or null
     */
    @Optional
    @Input
    String nugetPackageId

    PaketUpdate() {
        super(PaketUpdate.class)
        description = 'Update one or all dependencies to their latest version and update projects.'
        paketCommand = PaketCommand.UPDATE
        outputs.upToDateWhen { false }
    }

    @Override
    protected void configureArguments() {
        super.configureArguments()

        if (getNugetPackageId() != null) {
            args << getNugetPackageId()
        }
    }
}

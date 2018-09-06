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

package wooga.gradle.paket.get;

import wooga.gradle.paket.PaketIntegrationArgumentsSpec;
import wooga.gradle.paket.PaketPlugin;

import java.util.List;

class PaketGetArgumentsSpec extends PaketIntegrationArgumentsSpec {

    @Override
    Class getTestPlugin() {
        return PaketGetPlugin.class
    }

    @Override
    List<String> getTestTasks() {
        return [
                PaketGetPlugin.INSTALL_TASK_NAME,
                PaketGetPlugin.UPDATE_TASK_NAME,
                PaketGetPlugin.RESTORE_TASK_NAME,
                PaketGetPlugin.OUTDATED_TASK_NAME
        ]
    }
}

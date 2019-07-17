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

package wooga.gradle.paket.get

import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationBaseSpec

class PaketRestoreIntegrationSpec extends PaketIntegrationBaseSpec {

    def setup() {
        buildFile << """
            group = 'test'
            ${applyPlugin(PaketGetPlugin)}
        """.stripIndent()
    }

    @Override
    Object getBootstrapTestCases() {
        return [
                PaketGetPlugin.RESTORE_TASK_NAME
        ]
    }

    @Unroll
    def "task :#taskToRun fails when paket.lock is missing"() {
        given: "a paket dependency file"
        createFile("paket.dependencies") << """
            source https://nuget.org/api/v2
            
            nuget Mini
        """.stripIndent()

        when:
        def result = runTasksWithFailure(taskToRun)

        then:
        outputContains(result,"specified for property 'paketLock' does not exist")

        where:
        taskToRun << [PaketGetPlugin.RESTORE_TASK_NAME]
    }

    @Shared
    private List<String> paketDirectories = ["packages", ".paket", "paket-files"]

    @Unroll
    def "restores when #dirToDelete is missing"() {
        given: "a paket dependency file"
        createFile("paket.dependencies") << """
            source https://nuget.org/api/v2
            
            nuget Mini
        """.stripIndent()

        and: "the future paket files"
        def paketDirectories = paketDirectories.collect {new File(projectDir, it)}
        def restoreCacheFile = new File(projectDir, "paket-files/paket.restore.cached")

        and: "a paket install run to create the lock file and packages directory"
        runTasksSuccessfully("paketInstall")

        assert paketDirectories.every {it.exists()}

        when: "deleting packages directory"
        new File(projectDir, dirToDelete).delete()

        and: "running paket restore"
        runTasksSuccessfully(taskToRun)

        then:
        paketDirectories.every {it.exists()}

        where:
        taskToRun = PaketGetPlugin.RESTORE_TASK_NAME
        dirToDelete << paketDirectories
    }


    @Ignore //reason see issue https://github.com/wooga/atlas-paket/issues/53
    @Unroll
    def "task :#taskToRun caches last restore state"() {
        given: "a paket dependency file"
        createFile("paket.dependencies") << """
            source https://nuget.org/api/v2
            
            nuget Mini
        """.stripIndent()

        and: "the future paket files"
        def restoreCacheFile = new File(projectDir, "paket-files/paket.restore.cached")

        and: "a paket install run to create the lock file and packages directory"
        runTasksSuccessfully("paketInstall")

        and: "deleting packages directory"
        new File(projectDir, "packages").delete()

        and: "running paket restore"
        runTasksSuccessfully(taskToRun)

        when: "running restore again"
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasUpToDate(taskToRun)

        when: "deleting the cache file"
        restoreCacheFile.delete()
        result = runTasksSuccessfully(taskToRun)

        then:
        !result.wasUpToDate(taskToRun)

        where:
        taskToRun = PaketGetPlugin.RESTORE_TASK_NAME
    }
}

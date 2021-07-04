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

package wooga.gradle.paket

import nebula.test.functional.ExecutionResult
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.junit.Rule
import org.junit.contrib.java.lang.system.ProvideSystemProperty
import spock.lang.Shared

class IntegrationSpec extends nebula.test.IntegrationSpec{

    @Rule
    ProvideSystemProperty properties = new ProvideSystemProperty("ignoreDeprecations", "true")

    @Shared
    File cachedPaketDir

    def escapedPath(String path) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (osName.contains("windows")) {
            return StringEscapeUtils.escapeJava(path)
        }
        path
    }

    def convertToWindowsPath(String path) {
        new File(path).toString()
    }

    def setupSpec() {
        cachedPaketDir = File.createTempDir("paket","cache")
        cachedPaketDir.deleteOnExit()
    }

    def cleanupSpec() {
        cachedPaketDir.deleteDir()
    }

    def getPaketDir() {
        new File(projectDir, ".paket")
    }

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }

        if(isValidPaketDirectory(cachedPaketDir)) {
            FileUtils.copyDirectory(cachedPaketDir, paketDir)
        }
    }

    Boolean isValidPaketDirectory(File dir) {
        File bootstrapper = new File(dir, "paket.bootstrapper.exe")
        File paket = new File(dir, "paket.exe")
        bootstrapper.exists() && paket.exists()
    }

    void cleanupPaketDirectory() {
        File paketDir = paketDir
        if(paketDir.exists() && paketDir.isDirectory()) {
            paketDir.deleteDir()
        }
    }

    def cleanup() {
        //copy the .paket folder to cache directory
        File paketDir = paketDir

        if( isValidPaketDirectory(paketDir) ) {
            FileUtils.copyDirectory(paketDir, cachedPaketDir)
        }
    }

    Boolean outputContains(ExecutionResult result, String message) {
        result.standardOutput.contains(message) || result.standardError.contains(message)
    }
}

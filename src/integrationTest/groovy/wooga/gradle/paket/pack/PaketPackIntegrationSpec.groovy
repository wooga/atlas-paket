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

package wooga.gradle.paket.pack

import org.gradle.api.file.FileTree
import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationDependencyFileSpec
import wooga.gradle.paket.pack.tasks.PaketPack

class PaketPackIntegrationSpec extends PaketIntegrationDependencyFileSpec {

    def paketTemplateFile
    def version = "1.0.0"
    def packageID = "Wooga.Test"

    def setup() {
        buildFile << """
            group = 'test'
            version = "$version"
            ${applyPlugin(PaketPackPlugin)}
        """.stripIndent()

        paketTemplateFile = createFile("paket.template")
        paketTemplateFile << """
            type file
            id $packageID
            authors Wooga
            owners Wooga
            description
                Empty nuget package.
        """.stripIndent()

        createFile("paket.lock")
    }

    @Override
    Object getBootstrapTestCases() {
        ["paketPack-WoogaTest"]
    }

    @Unroll
    def "writes nuget packages to output directory when running #taskToRun"(String taskToRun) {
        given:
        def outputFile = new File(new File(new File(projectDir, 'build'), "outputs"), "${packageID}.${version}.nupkg")
        assert !outputFile.exists()

        and: "a empty paket.dependencies file"
        createFile("paket.dependencies")

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        outputFile.exists()
        result.wasExecuted("paketPack-WoogaTest")

        where:
        taskToRun << ["paketPack-WoogaTest", "buildNupkg", "assemble"]
    }

    @Unroll
    def "can depend on generated pack tasks #taskToRun"(String taskToRun) {
        given: "the build.gradle with second task that must run before packetPack"
        buildFile << """
            task(preStep) {
                doLast {
                    println("execute pre step")
                }
            }
            
            project.tasks["paketPack-WoogaTest"].dependsOn preStep
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.wasExecuted("paketPack-WoogaTest")
        result.wasExecuted("preStep")

        where:
        taskToRun << ["paketPack-WoogaTest", "buildNupkg", "assemble"]
    }

    def "skips pack task creation for duplicate package id"() {
        given: "some paket template files in the file system with same id"
        def subDir1 = new File(projectDir, "sub1")
        def subDir2 = new File(projectDir, "sub2")
        subDir1.mkdirs()
        subDir2.mkdirs()

        projectWithPaketTemplate(subDir1, "Test.Package1")
        projectWithPaketTemplate(subDir2, "Test.Package1")

        when: "applying paket-pack plugin"
        def result = runTasksSuccessfully(taskToRun)

        then:
        result.standardOutput.contains("Multiple paket.template files with id Test.Package1")
        result.standardOutput.contains("Template file with same id already in use")
        result.standardOutput.contains("Skip template file:")

        where:
        taskToRun << ["paketPack-TestPackage1", "buildNupkg", "assemble"]
    }

    @Unroll("verify sort order for template file duplication #subDir1Name|#subDir2Name|#subDir3Name")
    def "skips pack task creation for duplicate package id and uses first in sorted order"() {
        given: "some paket template files in the file system with same id"
        def subDir1 = new File(projectDir, subDir1Name)
        def subDir2 = new File(projectDir, subDir2Name)
        def subDir3 = new File(projectDir, subDir3Name)

        subDir1.mkdirs()
        subDir2.mkdirs()
        subDir3.mkdirs()

        def templateFiles = [
                projectWithPaketTemplate(subDir1, "Test.Package1"),
                projectWithPaketTemplate(subDir2, "Test.Package1"),
                projectWithPaketTemplate(subDir3, "Test.Package1")
        ]

        when: "applying paket-pack plugin"
        def result = runTasksSuccessfully("tasks")

        then:
        def expectedFileToUse = templateFiles.remove(expectedFileIndex)
        def unusedOne = templateFiles[0]
        def unusedTwo = templateFiles[1]
        result.standardOutput.contains("Multiple paket.template files with id Test.Package1")
        result.standardOutput.contains("Template file with same id already in use ${expectedFileToUse.path}")
        result.standardOutput.contains("Skip template file: ${unusedOne.path}")
        result.standardOutput.contains("Skip template file: ${unusedTwo.path}")

        where:
        subDir1Name   | subDir2Name   | subDir3Name   | expectedFileIndex
        "sub1/sub1"   | "sub2"        | "sub1/sub2"   | 1
        "sub1/subxzy" | "sub2/subklm" | "sub1/subabc" | 2
        "sub1"        | "sub2"        | "sub3"        | 0
        "sub1/sub1"   | "sub1"        | "sub1/sub2"   | 1
    }
}

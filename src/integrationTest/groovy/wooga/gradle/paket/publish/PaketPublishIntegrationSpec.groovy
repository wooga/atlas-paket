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

package wooga.gradle.paket.publish

import groovy.json.StringEscapeUtils
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.PaketIntegrationDependencyFileSpec
import wooga.gradle.paket.pack.PaketPackPlugin

class PaketPublishIntegrationSpec extends PaketIntegrationDependencyFileSpec {

    def paketTemplateFile

    @Shared
    def version = "1.0.0"

    def uniquPackagePostfix() {
        String key = "TRAVIS_JOB_NUMBER"
        def env = System.getenv()
        if (env.containsKey(key)) {
            return env.get(key)
        }
        return ""
    }

    @Shared
    def packageID = "Wooga.Test" + uniquPackagePostfix()

    @Shared
    def repoName = "integration"

    @Shared
    def packageName = "${packageID}.${version}.nupkg"

    @Shared
    def artifactoryUrl = "https://wooga.jfrog.io/wooga"

    @Shared
    Artifactory artifactory

    @Override
    Object getBootstrapTestCases() {
        ["publish-${packageIdToName(packageID)}", "publish${repoName.capitalize()}-${packageIdToName(packageID)}"]
    }

    def artifactoryRepoName = "atlas-nuget-integrationTest"
    def repoUrl = "$artifactoryUrl/api/nuget/atlas-nuget-integrationTest"
    def localPath

    def packageIdToName(id) {
        id.replaceAll(/\./, '')
    }

    def setupSpec() {
        String artifactoryCredentials = System.getenv("artifactoryCredentials")
        assert artifactoryCredentials
        def credentials = artifactoryCredentials.split(':')
        artifactory = ArtifactoryClientBuilder.create()
                .setUrl(artifactoryUrl)
                .setUsername(credentials[0])
                .setPassword(credentials[1])
                .build()
    }

    def setup() {
        buildFile << """
            group = 'test'
            version = "$version"

            ${applyPlugin(PaketPackPlugin)}
            ${applyPlugin(PaketPublishPlugin)}

            publishing {
                repositories {
                    nuget {
                        name "$repoName"
                        url "$repoUrl"
                    }
                }
            }

            paketPublish {
                publishRepositoryName = "$repoName"
            }

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

        localPath = File.createTempDir()
        localPath.deleteOnExit()
        cleanupArtifactory(artifactoryRepoName, packageName)
    }

    def cleanup() {
        cleanupArtifactory(artifactoryRepoName, packageName)
    }

    def cleanupArtifactory(String repoName, String artifactName) {
        List<RepoPath> searchItems = artifactory.searches()
                .repositories(repoName)
                .artifactsByName(artifactName)
                .doSearch()

        for (RepoPath searchItem : searchItems) {
            String repoKey = searchItem.getRepoKey()
            String itemPath = searchItem.getItemPath()
            artifactory.repository(repoName).delete(itemPath)
        }
    }

    def hasPackageOnArtifactory(String repoName, String artifactName) {
        List<RepoPath> packages = artifactory.searches()
                .repositories(repoName)
                .artifactsByName(artifactName)
                .doSearch()

        assert packages.size() == 1
        true
    }

    @Unroll
    def 'builds package and publish when running tasks #taskToRun'(String taskToRun) {
        given: "the future npkg artifact"
        def nugetArtifact = new File(new File(new File(projectDir, 'build'), "outputs"), packageName)
        assert !nugetArtifact.exists()

        and: "paket.dependencies and paket.lock file"
        createFile("paket.lock")
        createFile("paket.dependencies")

        when: "run the publish task"
        def result = runTasksSuccessfully(taskToRun)

        then:
        nugetArtifact.exists()
        result.wasExecuted("paketPack-${packageIdToName(packageID)}")
        hasPackageOnArtifactory(artifactoryRepoName, packageName)

        where:
        taskToRun << ["publish-${packageIdToName(packageID)}", "publish${repoName.capitalize()}-${packageIdToName(packageID)}", "publish${repoName.capitalize()}", "publish"]
    }

    @Unroll
    def "publish package with custom endpoint #endpoint"() {
        given: "the future npkg artifact"
        def nugetArtifact = new File(new File(new File(projectDir, 'build'), "outputs"), packageName)
        assert !nugetArtifact.exists()

        and: "paket.dependencies and paket.lock file"
        createFile("paket.lock")
        createFile("paket.dependencies")

        and: "a build.gradle file with a custom nuget repo entry"
        buildFile.text = ""
        buildFile << """
            group = 'test'
            version = "$version"

            ${applyPlugin(PaketPackPlugin)}
            ${applyPlugin(PaketPublishPlugin)}

            publishing {
                repositories {
                    nuget {
                        name "$repoName"
                        url "$repoUrl"
                        endpoint "$endpoint"
                    }
                }
            }
            
            paketPublish {
                publishRepositoryName = "$repoName"
            }
        """.stripIndent()

        when: "run the publish task"
        def result = runTasks("publish")

        then:
        nugetArtifact.exists()
        result.standardOutput.contains("endpoint $endpoint")

        where:
        endpoint << ["/api/v2/package", "some/random/path"]
    }

    @Unroll
    def 'builds package and publish locally #taskToRun'(String taskToRun) {
        given: "the future npkg artifact"
        def nugetArtifact = new File(new File(new File(projectDir, 'build'), "outputs"), packageName)
        assert !nugetArtifact.exists()

        and: "paket.dependencies and paket.lock file"
        createFile("paket.lock")
        createFile("paket.dependencies")
        def escapedPath = escapedPath(localPath.absolutePath)

        and: "a build.gradle file with a local publish entry"
        buildFile.text = ""
        buildFile << """
            group = 'test'
            version = "$version"

            ${applyPlugin(PaketPackPlugin)}
            ${applyPlugin(PaketPublishPlugin)}

            publishing {
                repositories {
                    nuget {
                        name "$repoName"
                        path "$escapedPath"
                    }
                }
            }
            
            paketPublish {
                publishRepositoryName = "$repoName"
            }

        """.stripIndent()

        and: "a future local output file"
        def futureFile = new File(escapedPath, nugetArtifact.name)
        assert !futureFile.exists()

        when: "run the publish task"
        def result = runTasksSuccessfully(taskToRun)

        then:
        nugetArtifact.exists()
        result.wasExecuted("paketPack-${packageIdToName(packageID)}")
        futureFile.exists()

        where:
        taskToRun << ["publish-${packageIdToName(packageID)}", "publish${repoName.capitalize()}-${packageIdToName(packageID)}", "publish${repoName.capitalize()}", "publish"]
    }

    def escapedPath(String path) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (osName.contains("windows")) {
            return StringEscapeUtils.escapeJava(path)
        }
        path
    }
}

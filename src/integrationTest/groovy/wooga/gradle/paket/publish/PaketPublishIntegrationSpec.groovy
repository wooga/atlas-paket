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

package wooga.gradle.paket.publish

import nebula.test.IntegrationSpec
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.paket.pack.PaketPackPlugin

class PaketPublishIntegrationSpec extends IntegrationSpec {

    def paketTemplateFile

    @Shared
    def version = "1.0.0"

    @Shared
    def packageID = "Wooga.Test" + System.getenv().getOrDefault("TRAVIS_JOB_NUMBER", "")

    @Shared
    def repoName = "integration"

    @Shared
    def packageName = "${packageID}.${version}.nupkg"

    @Shared
    def artifactoryUrl = "https://wooga.jfrog.io/wooga"

    @Shared
    Artifactory artifactory

    def artifactoryRepoName = "atlas-nuget-integrationTest"
    def repoUrl = "$artifactoryUrl/api/nuget/atlas-nuget-integrationTest"

    def packageIdToName(id) {
        id.replaceAll(/\./, '')
    }

    def setupSpec() {
        String artifactoryCredentials = System.getenv("artifactoryCredentials")
        assert artifactoryCredentials
        def credentials = artifactoryCredentials.split(':')
        artifactory = ArtifactoryClient.create(artifactoryUrl, credentials[0], credentials[1])
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

        createFile("paket.lock")
        createFile("paket.dependencies")

        cleanupArtifactory(artifactoryRepoName,packageName)
    }

    def cleanup() {
        cleanupArtifactory(artifactoryRepoName,packageName)
    }

    def cleanupArtifactory(String repoName,String artifactName) {
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

    def hasPackageOnArtifactory(String repoName,String artifactName) {
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

        when: "run the publish task"
        def result = runTasksSuccessfully(taskToRun)

        then:
        nugetArtifact.exists()
        result.wasExecuted("paketPack-${packageIdToName(packageID)}")
        hasPackageOnArtifactory(artifactoryRepoName, packageName)

        where:
        taskToRun << ["publish-${packageIdToName(packageID)}", "publish${repoName.capitalize()}-${packageIdToName(packageID)}", "publish${repoName.capitalize()}", "publish"]
    }
}

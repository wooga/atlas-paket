#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([usernameColonPassword(credentialsId: 'artifactory_publish', variable: 'artifactory_publish'),
                 usernameColonPassword(credentialsId: 'artifactory_deploy', variable: 'artifactory_deploy'),
                 string(credentialsId: 'atlas_paket_coveralls_token', variable: 'coveralls_token'),
                 string(credentialsId: 'atlas_plugins_sonar_token', variable: 'sonar_token'),
                 string(credentialsId: 'atlas_plugins_snyk_token', variable: 'SNYK_TOKEN')]) {

    def testEnvironment = [
                            "artifactoryCredentials=${artifactory_publish}",
                            "nugetkey=${artifactory_deploy}"
                          ]

    buildGradlePlugin platforms: ['macos','windows','linux'], coverallsToken: coveralls_token, sonarToken: sonar_token, testEnvironment: testEnvironment
}

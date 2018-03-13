Paket-Publish
=============

This plugin allows to publish `.nupkg` packages packed with `paket-pack` to nuget repositories.
It will apply the plugins `publishing` and `paket-base` plugin.

Tasks
-----

The plugin creates a number of tasks based on the configurated publishing repositories and artifacts inside the project.

| Task name                           | Depends on                           | Type                                              | Description |
| ----------------------------------- | ------------------------------------ | ------------------------------------------------- | ----------- |
| publish-*packageId*                 | the `npkg` artifact, paketBootstrap  | `wooga.gradle.paket.publish.tasks.PaketPush`      | publishes one artifact from the `npgk` configuration based on the `packageId` to the default publishing repository
| publish*repositoryName*-*packageId* | the `npkg` artifact, paketBootstrap  | `wooga.gradle.paket.publish.tasks.PaketPush`      | publishes one artifact from the `npgk` configuration based on the `packageId` to the repository named: `repositoryName`
| publish*repositoryName*             | all `nppg` artifacts, paketBootstrap | `DefaultTask`                                     | publishes all artifacts to the publishing repository named: `repositoryName`

Publish Repositories
--------------------

Before you can use and publish `npkg` packages you need to configure one or more publish repositories. The `paket-publish` plugin will hook itself into the base publishing plugin of gradle.

**build.gradle**

```gradle
plugins {
    id "net.wooga.paket-publish" version "0.6.0"
}

publishing {
    repositories {
        nuget {
            name "internal name of the repository"
            url "url to repository"
            apiKey = "optional api key"
            endpoint = "optional endpoint" //default /api/v2/package
        }
    }
}
```

By default [`Paket`][paket_push] will try to use the `nugetkey` environment variable for authentication. If you specify the `apikey` value it will be passed to the [`paket push`][paket_push] command.

The plugin will hook itself into the publishing lifecycle and will make the `publish` task depending on `publish*publishRepositoryName*`. This allows to publish multiple packages to the same repository with one task.

**build.gradle**
```gradle
plugins {
    id "net.wooga.paket-publish" version "0.6.0"
}

publishing {
    ...
}

paketPublish {
    publishRepositoryName = "snapshot" // defaults to "nuget"
}

```

The value of `publishRepositoryName` can also be set via gradle parameter `paket.publish.repository`

**terminal**

```
$./gradlew -Ppaket.publish.repository=snapshot publish

```

<!-- Links -->
[paket_push]:           https://fsprojects.github.io/Paket/paket-push.html
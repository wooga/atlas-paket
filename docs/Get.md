Paket-Get
=========

The `paket-get` plugin will apply the `paket-base` plugin and all it's tasks.
If you only want to retrieve dependencies through paket in your project apply this plugin. It will check for a `paket.dependencies` file in your root. The tasks provided will skip with `NO-SOURCES` when there is no `paket.dependencies` file.
Check out the documentation of `Paket` to learn more.

Tasks
-----

The `paket-get` plugin adds a number of tasks to your project. These are mainly commandline wrappers for the corresponding [Paket][paket] task.

| Task name      | Depends on     | Type                                           | Description |
| -------------- | -------------- | ---------------------------------------------- | ----------- |
| paketInstall   | paketBootstrap | `wooga.gradle.paket.get.tasks.PaketInstall`    | Download the dependencies specified by the paket.dependencies or paket.lock file into the packages/ directory and update projects.|
| paketRestore   | paketBootstrap | `wooga.gradle.paket.get.tasks.PaketRestore`    | Download the dependencies specified by the paket.lock file into the `packages/` directory. |
| paketUpdate    | paketBootstrap | `wooga.gradle.paket.get.tasks.PaketUpdate`     | Update one or all dependencies to their latest version and update projects |

Dependency update tasks
-----------------------

The plugin also generates one or more hidden update task for every dependency in the `paket.dependencies` file.

**paket.dependencies**
```
source https://nuget.org/api/v2

nuget DependencyOne
nuget DependencyTwo
```

Gradle will generate next to the `paketUpdate` task also:

* `paketUpdateDependencyOne`
* `paketUpdateDependencyTwo`

The task will be visible with `gradle tasks --all`

[paket]:                https://fsprojects.github.io/Paket/ "Paket"

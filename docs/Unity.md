Paket-Unity
===========

This plugin allows to use the extension [paket.unity3d] together with `paket-get`

Tasks
-----

The `paket-unity` plugin adds a few tasks that will hook themself onto `paket-get`

| Task name           | Depends on          | Type                                                | Description |
| ------------------- | ------------------- | --------------------------------------------------- | ----------- |
| paketUnityInstall   | | `wooga.gradle.paket.unity.tasks.PaketUnityInstall`  | Installs the dependencies into the Unity3d project

The `paketUnityInstall` will configure itself as a [`finalizedBy`][gradle_finalizedBy] task for `paketInstall`, `paketRestore` and `paketUpdate`. There is no need to call this task manually. The task also gets skipped when no `paket.unity3d.references` file can be found anywhere in the project directory tree.

[gradle_finalizedBy]:   https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:finalizedBy
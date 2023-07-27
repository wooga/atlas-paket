Paket-Unity
===========

This plugin allows to use the extension [paket.unity3d] together with `paket-get`

Tasks
-----

The `paket-unity` plugin adds a few tasks that will hook themself onto `paket-get`

| Task name         | Depends on | Type                                               | Description                                        |
|-------------------|------------|----------------------------------------------------|----------------------------------------------------|
| paketUnityInstall |            | `wooga.gradle.paket.unity.tasks.PaketUnityInstall` | Installs the dependencies into the Unity3d project |

The `paketUnityInstall` will configure itself as a [`finalizedBy`][gradle_finalizedBy] task
for `paketInstall`, `paketRestore` and `paketUpdate`. There is no need to call this task manually. The task also gets
skipped when no `paket.unity3d.references` file can be found anywhere in the project directory tree.


Extension
---------

The `paketUnity` extension provided by the plugin can be used for configuration. It has all properties
from `PaketPluginExtension`, plus the ones below:

| Property                       | Type                                    | Description                                                                                     |
|--------------------------------|-----------------------------------------|-------------------------------------------------------------------------------------------------|
| paketReferenceFiles            | `FileCollection` (Read only)            | A list of all `paket.unity3D.references` files                                                  |
| paketOutputDirectoryName       | `String`                                | Output directory for the paket installation. Relative to `<unity_project>/Assets`.              |
| assemblyDefinitionFileStrategy | `AssemblyDefinitionFileStrategy` (Enum) | Strategy regarding assemble definition files.                                                   |
| includeAssemblyDefinitions     | `Boolean`                               | Whether assembly definition files should be included during installation.                       |
| paketUpmPackageEnabled         | `Property<Boolean>`                     | Enables/Disables UPM package mode.                                                              |
| paketUpmPackageManifests       | `MapProperty<String, Map>`              | Maps `[paketName: upmPackageManifestMap]` for package.json files generated in UPM package mode. |


UPM mode
--------

Paket packages can also be configured to be installed as UPM packages. This can be useful when your paket package
contains an UPM one (with a `package.json` file).
This mode sets the installation directory (`paketOutputDirectoryName`) to the unity project's `Packages` folder, and
ensures that the `package.json`
file for each dependency is in the installed package root.

If an installed package is not UPM-compatible, that is, it doesn't have a `package.json` file, such file will be created. 
By default, a `package.json` is generated with `com.wooga.nuget.${paketPackage.toLowerCase()}` name, and version `0.0.0`, 
but those can be overridden and more properties can be added on using the mappings in `paketUpmPacakgeJson`. 

UPM mode can be enabled through the `paketUpmPackageEnabled` property, or using the `paketUnity.enablePaketUpmPackages()` extension
method. It is disabled by default.

[gradle_finalizedBy]:   https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:finalizedBy
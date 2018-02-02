Paket-Pack
==========

The `paket-pack` plugin allows you to package `.nugkg` packages from [`paket.template`][paket_template] files.
It will apply the plugins `base` and `paket-base`.

Tasks
-----

The plugin will generate a package task based on the package id inside the [`paket.template`][paket_template]  file

| Task name             | Depends on          | Type                                              | Description |
| --------------------- | ------------------- | ------------------------------------------------- | ----------- |
| paketPack-*packageId* | paketBootstrap      | `wooga.gradle.paket.pack.tasks.PaketPack`         | Packs the [`paket.template`][paket_template]  file into a `nupkg` package |

The plugin checks if the `paket-get` plugin is applied. If so, it will make all `PaketPack` tasks [`dependOn`][gradle_dependsOn] `paketInstall`.

Version for package
-------------------

The default value for the version parameter in the `PaketPack` task will be determined like:

1. use version in `paket.template` if available
2. fallback to `project.version` if available (`!=unspecified`)

If the default value for version property could not be set and the task was not configured with a custom version, `gradle` will fail.

Artifacts
---------

When you apply the `paket-pack` plugin your project will produce artifacts in the `nupkg` configuration.
You can access these artifacts from subprojects or inside your build.gradle

**build.gradle**

```
task(copyToSomeWhere, type:Copy) {
    from {configurations.nupkg}
    into 'custom/output'
}

```

<!-- Links -->
[paket_template]:       https://fsprojects.github.io/Paket/template-files.html "paket template file"
[gradle_dependsOn]:     https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:dependsOn
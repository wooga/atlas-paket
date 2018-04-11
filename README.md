Atlas-Paket
===========

[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.paket-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.paket)
[![Build Status](https://img.shields.io/travis/wooga/atlas-paket/master.svg?style=flat-square)](https://travis-ci.org/wooga/atlas-paket)
[![Coveralls Status](https://img.shields.io/coveralls/wooga/atlas-paket/master.svg?style=flat-square)](https://coveralls.io/github/wooga/atlas-paket?branch=master)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-paket/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-paket.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-paket.svg?style=flat-square)]()

This plugin provides tasks for retrieving and publishing [Paket][paket] packages in [gradle][gradle]. It downloads [`paket.exe`][paket_exe] via the [`paket.bootstrapper.exe`][paket_bootstrapper] and runs both with `mono/.Net`. This plugins helps to build and package .Net projects with gradle.

System Requirements
===================

### Unix
mono > 5.x

### Windows
.Net > 5.x

Applying the plugin
===================

**build.gradle**
```groovy
plugins {
    id 'net.wooga.paket' version '1.1.0'
}
```
-or-
```groovy
plugins {
    id 'net.wooga.paket-get' version '1.1.0'
    id 'net.wooga.paket-pack' version '1.1.0'
    id 'net.wooga.paket-publish' version '1.1.0'
    id 'net.wooga.paket-unity' version '1.1.0'
}
```

Documentation
=============

- [API docs](https://wooga.github.io/atlas-paket/docs/api/)
- [paket-base](docs/Base.md)
- [paket-get](docs/Get.md)
- [paket-pack](docs/Pack.md)
- [paket-publish](docs/Publish.md)
- [paket-unity](docs/Unity.md)
- [Release Notes](RELEASE_NOTES.md)

Gradle and Java Compatibility
=============================

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version | Works  |
| :------------: | :----: |
| < 3.0          | ![no]  |
| 3.0            | ![yes] |
| 3.1            | ![yes] |
| 3.2            | ![yes] |
| 3.3            | ![yes] |
| 3.4            | ![yes] |
| 3.5            | ![yes] |
| 3.5.1          | ![yes] |
| 4.0            | ![yes] |
| 4.1            | ![yes] |
| 4.2            | ![yes] |
| 4.3            | ![yes] |
| 4.4            | ![yes] |
| 4.5            | ![yes] |
| 4.6            | ![yes] |

Development
===========

[Code of Conduct](docs/Code-of-conduct.md)

LICENSE
=======

Copyright 2017 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

<!-- Links -->
[paket_template]:       https://fsprojects.github.io/Paket/template-files.html "paket template file"
[paket]:                https://fsprojects.github.io/Paket/ "Paket"
[paket_exe]:            https://github.com/fsprojects/Paket/releases/download/5.1.8/paket.exe

[paket_bootstrapper]:   https://github.com/fsprojects/Paket/releases/download/5.1.8/paket.bootstrapper.exe
[paket_unity3d]:        http://wooga.github.io/Paket.Unity3D/ "Paket.Unity3d"
[gradle]:               https://gradle.org/ "Gradle"
[gradle_finalizedBy]:   https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:finalizedBy
[gradle_dependsOn]:     https://docs.gradle.org/3.5/dsl/org.gradle.api.Task.html#org.gradle.api.Task:dependsOn

[yes]:              https://atlas-resources.wooga.com/icons/icon_check.svg "yes"
[no]:               https://atlas-resources.wooga.com/icons/icon_uncheck.svg "no"

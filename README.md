atlas-paket
===========

[![Build Status](https://travis-ci.org/wooga/atlas-paket.svg?branch=master)](https://travis-ci.org/wooga/atlas-paket)
[![Coverage Status](https://coveralls.io/repos/github/wooga/atlas-paket/badge.svg?branch=master)](https://coveralls.io/github/wooga/atlas-paket?branch=master)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-release-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

This plugin provides tasks for retrieving and publishing [Paket] (https://fsprojects.github.io/Paket/) packages in [gradle] (https://gradle.org/)

# Applying the plugin

    plugins {
        id 'net.wooga.paket' version '0.5.0'
    }
-or-
    
    plugins {
        id 'net.wooga.paket-get' version '0.5.0'
        id 'net.wooga.paket-pack' version '0.5.0'
        id 'net.wooga.paket-publish' version '0.5.0'
        id 'net.wooga.paket-unity' version '0.5.0'
    }


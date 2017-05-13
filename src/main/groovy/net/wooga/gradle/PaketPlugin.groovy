package net.wooga.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class PaketPlugin implements Plugin<Project>  {
    @Override
    void apply(Project project) {
        Task t = project.tasks.create('nooky',DefaultTask)
        t.group = 'Paket'
    }
}

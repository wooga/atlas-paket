package wooga.gradle.paket.pack.internal

import org.gradle.api.Project
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension

class DefaultPaketPackPluginExtension extends DefaultPaketPluginExtension {
    DefaultPaketPackPluginExtension(Project project) {
        super(project)
    }

    @Override
    String getVersion() {
        customVersion ?: project.version != Project.DEFAULT_VERSION ? project.version.toString() : null
    }
}
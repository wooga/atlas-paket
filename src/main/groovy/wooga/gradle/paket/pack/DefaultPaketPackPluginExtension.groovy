package wooga.gradle.paket.pack

import org.gradle.api.Project
import wooga.gradle.paket.base.DefaultPaketPluginExtension

class DefaultPaketPackPluginExtension extends DefaultPaketPluginExtension {
    DefaultPaketPackPluginExtension(Project project) {
        super(project)
    }

    @Override
    String getVersion() {
        customVersion ?: project.version != Project.DEFAULT_VERSION ? project.version.toString() : null
    }
}

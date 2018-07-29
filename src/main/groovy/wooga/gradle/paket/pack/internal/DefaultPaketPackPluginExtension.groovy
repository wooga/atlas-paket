package wooga.gradle.paket.pack.internal

import org.gradle.api.Project
import wooga.gradle.paket.base.dependencies.PaketDependencyHandler
import wooga.gradle.paket.base.internal.DefaultPaketPluginExtension

class DefaultPaketPackPluginExtension extends DefaultPaketPluginExtension {
    DefaultPaketPackPluginExtension(Project project, final PaketDependencyHandler dependencyHandler) {
        super(project, dependencyHandler)
    }

    @Override
    String getVersion() {
        customVersion ?: project.version != Project.DEFAULT_VERSION ? project.version.toString() : null
    }
}

package wooga.gradle.paket.get;

import wooga.gradle.paket.PaketIntegrationArgumentsSpec;
import wooga.gradle.paket.PaketPlugin;

import java.util.List;

class PaketGetArgumentsSpec extends PaketIntegrationArgumentsSpec {

    @Override
    Class getTestPlugin() {
        return PaketGetPlugin.class
    }

    @Override
    List<String> getTestTasks() {
        return [
                PaketGetPlugin.INSTALL_TASK_NAME,
                PaketGetPlugin.UPDATE_TASK_NAME,
                PaketGetPlugin.RESTORE_TASK_NAME,
                PaketGetPlugin.OUTDATED_TASK_NAME
        ]
    }
}

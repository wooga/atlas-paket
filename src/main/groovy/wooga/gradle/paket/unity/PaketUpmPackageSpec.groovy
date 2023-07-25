package wooga.gradle.paket.unity

import com.wooga.gradle.BaseSpec
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

trait PaketUpmPackageSpec implements BaseSpec {

    private final MapProperty<String, Map<String, Object>> paketUpmPackageManifests = objects.mapProperty(String, Map)

    /**
     * Map [<paket_package_name>: <upm_manifest_contents>] that overrides the contents of generated package.json files in UPM package mode.
     */
    @Input
    @Optional
    MapProperty<String, Map<String, Object>> getPaketUpmPackageManifests() {
        return paketUpmPackageManifests
    }

    void setPaketUpmPackageManifests(Map paketUpmPackages) {
        this.paketUpmPackageManifests.set(paketUpmPackages)
    }

    void setPaketUpmPackageManifests(Provider<Map<String, String>> paketUpmPackages) {
        this.paketUpmPackageManifests.set(paketUpmPackages)
    }


    private final Property<Boolean> paketUpmPackageEnabled = objects.property(Boolean)

    /**
     *
     * Enables "UPM package mode" for paket. This mode searches for a folder with a 'package.json' in the paket project,
     * and installs the package in the 'Packages' folder, using the discovered folder as root.
     * <br>
     * Non-UPM compatible packages will be given a basic package.json file with a package name described in the
     * {@code paketUpmPackages} property. If a mapping is not found there,
     * a generic 'com.wooga.nuget.<paket-package-name>' name will be used.
     * <br>
     */
    void enablePaketUpmPackages() {
        paketUpmPackageEnabled.set(true)
    }

    /**
     * Enables/Disables "UPM package mode" for paket. See {@code PaketUpmPackageSpec::enablePaketUpmPackages} for more details.
     */
    @Input
    @Optional
    Property<Boolean> getPaketUpmPackageEnabled() {
        return paketUpmPackageEnabled
    }

    Property<Boolean> isPaketUpmPackageEnabled() {
        return paketUpmPackageEnabled
    }

    void setPaketUpmPackageEnabled(Boolean enablePaketUpmPackages) {
        this.paketUpmPackageEnabled.set(enablePaketUpmPackages)
    }

    void setPaketUpmPackageEnabled(Provider<Boolean> enablePaketUpmPackages) {
        this.paketUpmPackageEnabled.set(enablePaketUpmPackages)
    }
}

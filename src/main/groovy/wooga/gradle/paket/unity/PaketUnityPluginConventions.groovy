package wooga.gradle.paket.unity

import com.wooga.gradle.PropertyLookup
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy

class PaketUnityPluginConventions {
     static final AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy = AssemblyDefinitionFileStrategy.disabled
     static final PropertyLookup paketUpmPackageEnabled = new PropertyLookup(
             "PAKET_UNITY_ENABLE_UPM_PACKAGE",
             "paketUnity.enableUpmPackage",
             false
     )
}

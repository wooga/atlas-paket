package wooga.gradle.paket.unity

import com.wooga.gradle.PropertyLookup
import wooga.gradle.paket.unity.internal.AssemblyDefinitionFileStrategy

class PaketUnityPluginConventions {

     /**
      * Default strategy for handling the assembly definition files
      */
     static final AssemblyDefinitionFileStrategy assemblyDefinitionFileStrategy = AssemblyDefinitionFileStrategy.disabled

     /**
      * Whetehr to enable upm package support
      */
     static final PropertyLookup paketUpmPackageEnabled = new PropertyLookup(
             "PAKET_UNITY_ENABLE_UPM_PACKAGE",
             "paketUnity.enableUpmPackage",
             false
     )

     /**
      * Default NET frameworks that are to be installed onto the target directory
      */
     static final List<String> defaultFrameworks = ["net11", "net20", "net35", "net45", "netstandard2.0"]
}

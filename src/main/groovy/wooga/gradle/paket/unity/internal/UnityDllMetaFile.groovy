package wooga.gradle.paket.unity.internal

/***
 * A representation of a .meta file generated by Unity during an import
 */
class UnityDllMetaFile {

    String guid
    Boolean autoLoaded = true
    Boolean validateReferences = false

    @Override
    String toString() {
        return """fileFormatVersion: 2
guid: ${guid}
PluginImporter:
  externalObjects: {}
  serializedVersion: 2
  iconMap: {}
  executionOrder: {}  
  isPreloaded: 0
  isOverridable: 0
  isExplicitlyReferenced: ${(autoLoaded ? "0" : "1")}
  validateReferences: ${(validateReferences ? "1" : "0")}
  platformData:
  - first:
      Any:
    second:
      enabled: 1
      settings: {}
  - first:
      Editor: Editor
    second:
      enabled: 0
      settings:
        DefaultValueInitialized: true
  - first:
      Windows Store Apps: WindowsStoreApps
    second:
      enabled: 0
      settings:
        CPU: AnyCPU
  userData: 
  assetBundleName: 
  assetBundleVariant: 
        """.stripIndent()
    }

    static File generate(String name, File directory) {
        def metaFile = new UnityDllMetaFile()

        def uuid = UUID.randomUUID()
        metaFile.guid = uuid.toString().replace("-", "")

        def file = new File(directory, "${name}.meta")
        file.createNewFile()
        file.write(metaFile.toString())
        file
    }

    static File generate(File dll) {
        generate(dll.name, dll.parentFile)
    }

}

package wooga.gradle.paket.base.utils.internal

class PaketUnityReferences {

    public List<String> nugets

    PaketUnityReferences(File referencesFile) {
        this(referencesFile.text)
    }

    PaketUnityReferences(String referencesContent) {
        nugets = []
        referencesContent.eachLine { line ->

            if(!line.empty){
                nugets << line
            }
        }
    }
}

package wooga.gradle.paket.base.utils.internal

class PaketLock {

    public static final String INDENTATION = ' ' * 2

    enum SourceType {
        NUGET("NUGET")

        private final String value

        SourceType(String value) {
            this.value = value
        }

        String getValue() {
            value
        }
    }

    private def content

    PaketLock(File lockFile) {
        this(lockFile.text.stripIndent())
    }

    PaketLock(String lockContent) {
        content = [:]

        def currentSourceType
        def currentPackageName

        lockContent.eachLine { line ->
            if (!line.trim() || line.trim().startsWith("remote:")) {
                return
            }
            if (isValidSourceType(line.trim())) {
                currentSourceType = line.trim()
                content[currentSourceType] = [:]
                return
            }
            if (line.startsWith(INDENTATION * 3)) {
                (content[currentSourceType][currentPackageName] as List<String>) << line.trim().split(" ")[0]
                return
            }
            if (line.startsWith(INDENTATION * 2)) {
                currentPackageName = line.trim().split(" ")[0]
                content[currentSourceType][currentPackageName] = []
                return
            }

        }
        println(content)
    }

    boolean isValidSourceType(String value) {

        for (SourceType type in SourceType.values()) {
            if (type.value == value) return true
        }
        return false
    }

    List<String> getDependencies(SourceType source, String id) {
        content[source.getValue()][id] as List<String>
    }
}

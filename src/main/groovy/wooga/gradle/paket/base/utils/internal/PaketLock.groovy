package wooga.gradle.paket.base.utils.internal

class PaketLock {

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

    enum LineType {

        TYPE(0), REMOTE(1), NAME(2), DEPENDENCY(3)

        private final int value

        LineType(int value) {
            this.value = value
        }

        int getValue() {
            value
        }
    }

    private def content

    PaketLock(File lockFile) {
        this(lockFile.text.stripIndent())
    }

    PaketLock(String lockContent) {
        content = [:]

        String currentSourceType
        String currentPackageName
        int currentIndent = 0
        String currentLineData

        lockContent.eachLine { line ->

            currentLineData = line.trim()
            if (currentLineData.trim().empty) {
                return
            }

            def match = (line =~ /^[\s]+/)
            currentIndent = !match ? 0 : match[0].size() / 2

            switch (currentIndent) {
                case LineType.TYPE.value:
                    if(isValidSourceType(currentLineData)) {
                        currentSourceType = currentLineData
                        content[currentSourceType] = [:]
                    }
                    break
                case LineType.NAME.value:
                    currentPackageName = currentLineData.split(/\s/)[0]
                    if(currentSourceType && currentPackageName) {
                        content[currentSourceType][currentPackageName] = []
                    }
                    break

                case LineType.DEPENDENCY.value:
                    if(currentSourceType && currentPackageName) {
                        (content[currentSourceType][currentPackageName] as List<String>) << currentLineData.split(/\s/)[0]
                    }
                    break

                case LineType.REMOTE.value:
                default:
                    break
            }
        }
    }

    static boolean isValidSourceType(String value) {

        for (SourceType type in SourceType.values()) {
            if (type.value == value) return true
        }
        return false
    }

    List<String> getDependencies(SourceType source, String id) {
        content[source.getValue()] && content[source.getValue()][id] ? content[source.getValue()][id] as List<String> : []
    }

    List<String> getAllDependencies(List<String> references) {
        def ref = references.collect { reference ->
            [reference, getAllDependencies(getDependencies(SourceType.NUGET, reference))]
        }

        ref.flatten().unique()
    }
}

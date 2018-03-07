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

        def currentSourceType
        def currentPackageName
        def currentIndent = 0
        def currentLeadingWhitespaces = 0
        def currentLineData

        lockContent.eachLine { line ->

            if (!line.trim()) {
                return
            }

            def newLeadingWhitespaces = (line =~ /\s/).size()
            if (newLeadingWhitespaces > currentLeadingWhitespaces) {
                currentIndent++
            } else if (newLeadingWhitespaces < currentLeadingWhitespaces) {
                currentIndent--
            }
            currentLeadingWhitespaces = newLeadingWhitespaces
            currentLineData = line.trim()

            if (currentIndent == LineType.TYPE.value && isValidSourceType(currentLineData)) {
                currentSourceType = currentLineData
                content[currentSourceType] = [:]
                return
            }
            if (currentIndent == LineType.REMOTE.value) {
                return
            }
            if (currentIndent == LineType.NAME.value) {
                currentPackageName = currentLineData.split(" ")[0]
                content["${currentSourceType}"][currentPackageName] = []
                return
            }
            if (currentIndent == LineType.DEPENDENCY.value) {
                (content["${currentSourceType}"]["${currentPackageName}"] as List<String>) << currentLineData.split(" ")[0]
            }
        }
    }

    boolean isValidSourceType(String value) {

        for (SourceType type in SourceType.values()) {
            if (type.value == value) return true
        }
        return false
    }

    List<String> getDependencies(SourceType source, String id) {
        content[source.getValue()] && content[source.getValue()][id] ? content[source.getValue()][id] as List<String> : null
    }

    List<String> getAllDependencies(List<String> references) {
        def result = []
        for (def referenceDependency in references) {
            result.add(referenceDependency)
            def referenceDependencies = getDependencies(SourceType.NUGET, referenceDependency)
            if (referenceDependencies) {
                result.addAll(referenceDependencies)
                def dependencies = getAllDependencies(referenceDependencies)
                if (dependencies) {
                    result.addAll(dependencies)
                }
            }
        }
        result.unique()
    }
}

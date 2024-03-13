package wooga.gradle.paket

/**
 * Supported framework restriction.
 * <a href="https://fsprojects.github.io/Paket/dependencies-file.html#Framework-restrictions">Reference</a>
 */
enum FrameworkRestriction {

    Net35("net35"),
    Net45("net45"),
    NetStandard2("netstandard2.0")

    String getLabel(){
        label
    }
    private final String label

    FrameworkRestriction(String label) {
        this.label = label
    }
}

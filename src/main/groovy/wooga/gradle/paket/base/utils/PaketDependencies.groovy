package wooga.gradle.paket.base.utils

/**
 * Object representation of a {@code paket.dependencies} file.
 */
interface PaketDependencies {

    /**
     * Returns a {@link Set<String>} of sources configured in {@code paket.dependencies} file.
     *
     * @return  sources for paket to fetch dependencies from
     */
    Set<String> getSources()

    /**
     * Returns a {@link Set<String>} of {@code nuget} dependencies configured {@code paket.dependencies} file.
     *
     * @return all nuget dependencies in {@code paket.dependencies} file.
     */
    Set<String> getNugetDependencies()

    /**
     * Returns a {@link List<String>} of configured .NET frameworks.
     * @return list of .NET frameworks configured
     */
    List<String> getFrameworks()
}
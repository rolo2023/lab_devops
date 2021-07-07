#!groovy
package api.vtrack

interface IComponent {

    /**
     * Creates a build in Vtrack
     *
     * @param buildMap A Vtrack build dictionary
     *
     * @return IBuild The Vtrack Build representation
     */
    IBuild createBuild(Map buildMap)

    /**
     * Gets a list of vTrack builds of the component
     *
     * @param pageSize The maximun number of builds to retrieve. Default: 10
     *
     * @return IBuild[] The list of vTrack builds
     */
    IBuild[] getComponentBuilds(int pageSize)

    /**
     * Gets a vTrack build of the component
     *
     * @param buildId The id of the build to retrieve
     *
     * @return IBuild The vTrack build
     */
    IBuild getComponentBuild(String buildId)

    /**
     * Generates a dictionary representing a Vtrack build
     *
     * @param buildMap A map representing a Vtrack build
     *
     * @return Map The Vtrack build dictionary
     */
    Map generateBuildMap(Map buildMap)
}
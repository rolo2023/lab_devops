#!groovy
package api.vtrack

interface IBuild {

    /**
     * Inserts a deploy in Vtrack
     *
     * @param deployMap A Vtrack deploy dictionary
     *
     * @return IDeploy The Vtrack Deploy representation
     */
    IDeploy createDeploy(Map deployMap)

    /**
     * Generates a dictionary representing a Vtrack deploy
     *
     * @param deployMap A map representing a Vtrack deploy
     *
     * @return Map The Vtrack deploy dictionary
     */
    Map generateDeployMap(Map deployMap)
}
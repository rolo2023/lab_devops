#!groovy
package api.vtrack

interface INamespace {

    /**
     * Generates a dictionary representing a Vtrack component
     *
     * @param componentMap A map representing a Vtrack component
     *
     * @return Map The Vtrack component dictionary
     */
    Map generateComponentMap(Map componentMap)

    /**
     * Gets Vtrack component
     *
     * @param name The name of the Vtrack component
     * @param componentToCreateIfNone If a component dictionary is provided, it will be inserted in Vtrack if not found
     *
     * @return IComponent The Vtrack Component representation or null if not found
     */
    IComponent getComponent(String name, Map componentToCreateIfNone)

    /**
     * Returns all vTrack components of the Namespace
     *
     * @return IComponent list
     */
    IComponent[] getComponents()

    /**
     * Returns the vTrack components of the Namespace matching criteria based on map
     *
     * @param andRsql a map containing the key and value to filer, i.e. andRsql = ['vcs_repo': 'myurl', 'package_type': 'maven']
     *        
     * @return IComponent list
     */
    IComponent[] getComponents(Map andRsql)

    /**
     * Returns the vTrack components of the Namespace matching criteria based on raw RSQL
     *
     * @param rawSql a string containing the RSQL query, i.e., rawSql='vcs_repo=="myurl";package_type=="maven"'
     *        
     * @return IComponent list
     */
    IComponent[] getComponents(String rawSql)
}
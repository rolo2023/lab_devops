package workflow

import globals.Helpers


class JenkinsPluginMock {
    String shortName

    JenkinsPluginMock(name) {
        shortName = name
    }
}

class Utils {
    /**
     * We can use this to fake a plugin check during tests
     *
     * Example:
     * <code>
     *     mockPluginManager(['artifactory', 'git'])
     *     Helpers.pluginExists('artifactory') # true
     *     Helpers.pluginExists('samuel') # false
     * </code>
     *
     * @param pluginsToFind List of 'installed' plugins to fake
     */
    static void mockPluginManager(List pluginsToFind) {
        Helpers.plugins = pluginsToFind.collect { new JenkinsPluginMock(it) }
    }
    /**
     * We can use this to fake a groups check during tests
     *
     * Example:
     * <code>
     *     Utils.mockUsersManager(['BBVA_CO_NET_USERS', 'jira-software-users'])
     *     Helpers.accessUserGrass('BBVA_CO_NET_USERS') # true
     *     Helpers.accessUserGrass('bitbucket-users') # false
     * </code>
     *
     * @param usersGroups List of groups user to fake
     */

    static void mockUsersManager(List usersGroups) {
        Helpers.access = usersGroups
    }
}
import globals.*

/**
 * Wrapper for 'runTests' library'
 * This wrapper supports the following frameworks:
 *    1.- Backend Testing (mvn commands) [https://globaldevtools.bbva.com/bitbucket/projects/BGT/repos/backend-testing/browse]
 *    2.- Acis Framework (npm commands) [https://globaldevtools.bbva.com/bitbucket/projects/BGT/repos/e2e-js-framework/browse]
 *          - require: 'npm file credentials'
 *          - require: 'export DATAMANAGER_HEADERS="{\"Authorization\":\"<API-KEY>\"}"'
 *
 * Mandatory params:
 * - context:
 *  - artifactoryNpmCredentials: 'file' type credentials with .npmrc configured with Artifactory access
 *  - dockerRegistryCredentials: 'user/password' type credentials used to log to Docker Registry
 *  - testManagerCredentials: 'secret' type credential with an identified URL for test manager
 *  - dataManagerCredentials: 'secret' type credential for Data Manager
 *  - namespace
 *  - application
 *
 * Optional
 * - libraryVersion: Defaults to Settings.libRunTest
 * - context:
 *   - saucelabsCredentials: 'userpass' type credential for Saucelabs (backend testing not use it)
 *   - polyfemoApiCredentials: 'userpass' type credential for endpoint polyfemo (backend testing prod use it)
 *   - dockerRegistry: Defaults to ${Settings.ether.artifactory.registry}*   - saucelabsCredentials: 'user/password' credentials for Saucelabs, defaults to 'bot-saucelabs'
 *   - gitUrl: Repo to test, defaults to the current one
 *   - branchName: branch of that repo to test, defaults to current one
 */
def call(Map args) {
    fromMap(args).checkMandatory(['context', 'tests'])

    def context = args.context as Map
    if (!validateContext(context)) {
        return
    }

    // Set sane defaults
    context << [
            buildUrl      : env.BUILD_URL,
            branchName    : args.context.branchName ?: env.BRANCH_NAME,
            gitUrl        : args.context.gitUrl ?: Settings.repo.GIT_URL,
            branchName    : args.context.branchName ?: Settings.repo.GIT_BRANCH,
            dockerRegistry: args.context.dockerRegistry ?: Settings.ether.artifactory.registry
    ]

    def tests = validateTests(args.tests as List, context)
    if (!tests) {
        return
    }
    args.tests = tests

    log.debug "libRunTest :: Curated args :: ${args}"
    return runLibRunTests(args)
}

private Boolean validateContext(Map context) {
    if (!fromMap(context).checkMandatory(
            ['namespace', 'application', 'dataManagerCredentials',
            'artifactoryNpmCredentials', 'dockerRegistryCredentials', 'testManagerCredentials'])
    ) { return false }

    // Check that credentials exist, and that they are of the expected type
    def credentialsToValidate = [
            "file: ${context.artifactoryNpmCredentials}",
            "string: ${context.testManagerCredentials}",
            "userpass: ${context.dockerRegistryCredentials}",
            "string: ${context.dataManagerCredentials}",
    ]

    if(context.saucelabsCredentials){
        credentialsToValidate << "userpass: ${context.saucelabsCredentials}"
    }

    //check the credential to apiserver endpoint polyfemo prod
    if(context.polyfemoApiCredentials){
        credentialsToValidate << "userpass: ${context.polyfemoApiCredentials}"
    }

    Boolean invalidCredentials = credentialsToValidate.collect { it -> Helpers.credentialExists(it) }.any { !it }
    if (invalidCredentials) {
        exit "libRunTests :: Invalid credentials are sent"
        return false
    }

    true
}

/**
 * At least one test is mandatory, and each test need to have a valid 'runner'
 * @param testList
 * @return true if tests are valid
 */
private List validateTests(List testList, Map context) {
    if (!testList || testList.size() == 0) {
        exit 'libRunTest :: At least one test must be declared'
        return null
    }

    List tests = []
    for (test in testList) {
        if (!test?.runner) {
            exit "libRunTests :: each test needs to have a 'runner' defined"
            return null
        }

        Closure runnerMethod = getRunnerMethod(test.runner as Map, context)
        if (!runnerMethod) {
            return null
        }
        test.runner = runnerMethod

        tests << test
    }

    return tests
}

def fromStep(Map args) { call(args) }

private def runLibRunTests(Map args) {
    def libVersion = args.libraryVersion ?: Settings.libRunTests
    library "test_legacy@${libVersion}"

    return runTests(context: args.context, tests: args.tests)
}

private Closure getRunnerMethod(Map runner, Map context) {
    switch (runner?.type) {
        case 'spring':
            return getSpringRunner(runner, context)
        default:
            exit "libRunTests :: Tests need a runner type: ${runner}"
            return null
    }
}

private Closure getSpringRunner(Map runnerConfig, Map context) {

    def extraOptions = runnerConfig.parameters ?: ''
    def mavenCommand = runnerConfig.command ?: 'mvn --batch-mode -Dstyle.color=always -V -U clean verify -f acceptance'

    def agentContainsCredentials = runnerConfig.credentialsFromAgent ?: false
    def mavenCredentials = runnerConfig.credentials // ?: 'spring_co_orquidea_settings_file'

    if (!agentContainsCredentials && !Helpers.credentialExists('file', mavenCredentials)) {
        exit "libRunTests :: getSpringRunner :: Chosen credential ${mavenCredentials} does is not valid"
        return null
    }

    return { ctx ->

        def extraOptionsExpanded = Eval.me("ctx", ctx,
            (extraOptions.trim().startsWith('{')) ? extraOptions : "def extraOptionsExpanded=\"${extraOptions}\"")

        def command = "${mavenCommand} ${extraOptionsExpanded}"

        Helpers.log.debug "libRunTests :: getSpringRunner :: ctx.dataManagerCredentials = ${context.dataManagerCredentials}"

        //adding credentials file to agent
        withCredentials([file(credentialsId: context.artifactoryNpmCredentials, variable: 'NPMRC')]) {
            sh "cp \${NPMRC} ~/.npmrc"
        }

        //loading environment required for ACIS
        withCredentials([string(credentialsId: context.dataManagerCredentials, variable: 'dmApiKeyBotPolyfemo')]) {
            def exportHeaderToACIS = "DATAMANAGER_HEADERS={\"Authorization\":\"${dmApiKeyBotPolyfemo}\"}"

            List envList = []
            envList << "${exportHeaderToACIS}"
            withEnv(envList){
                if (!agentContainsCredentials && mavenCredentials) {
                    withCredentials([file(credentialsId: mavenCredentials, variable: 'settingsXml')]) {
                        sh "${command} -s ${settingsXml}"
                    }
                } else {
                    sh command
                }
            }
        }
    }
}

return this

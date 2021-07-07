package modules
import globals.*
import jenkins.Pipeline

static EphemeralsCps newCps() { new EphemeralsCps() }

class EphemeralsCps extends Pipeline {

    void init(){
        def moduleParams = Settings.modules.ephemerals.params ?: [:]
        moduleParams << Settings.test
        Settings.modules.ephemerals.params = moduleParams
        if(Settings.modules.ephemerals.enabled) setParams()
    }

    // Return the default value if 'default' is set in form
    private def getValueOnDefault(paramName){

        Map formDefaults = [
            TEST_ENABLE: 'false',
            TEST_IGNORE_RESULT: 'true',
            TEST_MIRRORING: 'true'
        ]

        String paramValue = params."${paramName}"
        if(paramValue == 'default'){
            return formDefaults.get(paramName)
        } else {
            return paramValue
        }
    }

    def setParams() {
        def form = FormHelper.newCps()
        Settings.addToParameters([
            choice(name: 'TEST_ENABLE',
                    choices: form.choiceGen(['default', 'true' , 'false'], 'default'),
                    description: 'Use this option to force/avoid the execution of the tests'),

            string(name: 'TEST_TAGS',
                    defaultValue: '',
                    description: 'Use this option to filter the test cases to run, i.e health'),

            choice(name: 'TEST_ENVIRONMENT',
                    choices: form.choiceGen([ 'ephemeral', 'virtual', 'desarrollo', 'integrado' ], 'ephemeral', false),
                    description: 'Use this option the control the target environment of the tests'),

            string(name: 'TEST_RUN_OPTIONS',
                    defaultValue: '',
                    description: 'Use this option to set extra options, i.e --sp ./node_modules/bbva-atpi-test/lib/specs/login.atpi.spec.js --sp ./bbva-keox-test/node_modules/bbva-atpi-test/lib/specs/login.emap.spec.js'),

            choice(name: 'TEST_IGNORE_RESULT',
                    choices: form.choiceGen(['default', 'true' , 'false'], 'default'),
                    description: 'Use this option to ignore the result of the tests'),

            choice(name: 'TEST_MIRRORING',
                    choices: form.choiceGen(['default', 'true' , 'false'], 'default'),
                    description: 'Use this option to mirror test changes to an multi-app test repository')
        ])
    }

    Boolean shouldTest() {
        def shouldRunModule = Settings.branch in Settings.modules.ephemerals.enabled_branches
        if (!shouldRunModule) {
            Helpers.log.info("Ephemerals :: CHECK :: Not executing module since ${Settings.branch} is not in ${Settings.modules.ephemerals.enabled_branches}")
            return
        }

        Boolean executable = true
        Boolean warnTestFileMissing = false
        String dbgMessage = ''
        if (getSetting('test.enable') == 'false') {
            dbgMessage = 'Tests are disabled by form choice'
            executable = false
        } else if (getValueOnDefault('TEST_ENABLE') == 'false') {
            dbgMessage = 'Tests are disabled by default on this branch'
            executable = false
        } else if (!fileExists("${workspace}/test/pom.xml")) {
            dbgMessage = 'Tests are enabled, but test/pom.xml does not exist'
            warnTestFileMissing = true
            executable = false
        } else {
            dbgMessage = 'Test will be performed'
            executable = true
        }

        if (warnTestFileMissing) {
            Helpers.log.warn('MAG :: CHECK :: test file test/pom.xml missing')
        } else {
            Helpers.log.debug("MAG :: CHECK :: ${dbgMessage}")
        }
        return executable
    }

    void stageRunExternal(testNodeEnv){
        withEnv(testNodeEnv){
            def testConf = Settings.test
            def module = Settings.modules.ephemerals
            def artifactCfg = Settings.artifact

            def artifactContextUrl = "${artifactCfg.getUploadUrl()}/${artifactCfg.file.name}".replace("${artifactCfg.server}/", '')
            def USE_ON_DEMAND = "ephemeral".equals(testConf.params.environment)
            def RUN_TESTS_SPECS_TAGS = testConf.params.tags ?: ''
            def RUN_TESTS_IGNORE_RESULT = ! "false".equals(testConf.params.ignore_result)
            def RUN_TESTS_RUNNER_OPTIONS = testConf.params.run_options ?: ''

            List externalParams = [
                        booleanParam(name: 'USE_ON_DEMAND', value: USE_ON_DEMAND),
                        string(name: 'CHANGE_APP_NAME', value: Settings.uuaa),
                        string(name: 'CHANGE_ARTIFACTS', value: artifactContextUrl),
                        string(name: 'CHANGE_BUILD_URL', value: env.BUILD_URL),
                        string(name: 'CHANGE_BRANCH_NAME', value: env.BRANCH_NAME),
                        string(name: 'CHANGE_GIT_URL', value: Settings.repo.GIT_URL),
                        string(name: 'RUN_TESTS_SPECS_TAGS', value: RUN_TESTS_SPECS_TAGS),
                        booleanParam(name: 'RUN_TESTS_IGNORE_RESULT', value: RUN_TESTS_IGNORE_RESULT),
                        string(name: 'RUN_TESTS_RUNNER_OPTIONS', value: RUN_TESTS_RUNNER_OPTIONS)
                    ]
            Helpers.log.debug("Calling ${module.test_job} with the following params:\n" + Helpers.dump(externalParams))
            build job: module.test_job, parameters: externalParams, wait: true
        }
    }

    void stageRunVirtual(testNode){
        node(testNode){
            repo { credentialsId = Settings.modules.repo.credentials_id }
            unstash 'packagezip'
            withCredentials([
                string(credentialsId: 'test-manager-url', variable: 'TEST_MANAGER_URL'),
                file(credentialsId: 'maven-settings', variable: 'M2_SETTINGS'),
                file(credentialsId: envMap.npmrc_file_id, variable: 'NPMRC'),
                usernamePassword(credentialsId: "bot-testing-art", usernameVariable: 'DOCKER_USR', passwordVariable: 'DOCKER_PSW'),
                usernamePassword(credentialsId: "bbva-testing-bot", usernameVariable: 'SAUCE_ACCESS_USR', passwordVariable: 'SAUCE_ACCESS_PSW'),
                [$class: 'AmazonWebServicesCredentialsBinding',credentialsId: 'aws-testing-bucket', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
            ]) {
                def dockerNetwork = "escenia-${env.GIT_COMMIT[0..6]}"
                def sauceTunnel = dockerNetwork
                sh "cp -f \${NPMRC} ~/.npmrc"
                sh "docker login globaldevtools.bbva.com:5000 -u ${DOCKER_USR} -p ${DOCKER_PSW}"
                sh "docker network create ${dockerNetwork} || true"
                sh "mvn --batch-mode -Dstyle.color=always -V -U clean install -Dmaven.test.skip=true -s ${M2_SETTINGS}"
                sh "mvn --batch-mode \
                    -Djansi.passthrough=true \
                    -Dstyle.color=always \
                    -V -U clean verify \
                    -f test \
                    -Dtest.tags=virtual \
                    -Ddocker.network=${dockerNetwork} \
                    -Dsauce.user=${SAUCE_ACCESS_USR} \
                    -Dsauce.passwd=${SAUCE_ACCESS_PSW} \
                    -Dsauce.tunnel=${sauceTunnel} \
                    -Ddata.namespace=spain-virtual \
                    -Dtest.manager.project=${Settings.uuaa} \
                    -Dtest.manager.branch=${env.BRANCH_NAME} \
                    -Dtest.manager.url=${env.TEST_MANAGER_URL} \
                    -Dtest.manager.build.url=${env.BUILD_URL} \
                    -s ${M2_SETTINGS}\
                ".trim()
                sh "mvn --batch-mode s3-upload:s3-upload -f test"
            }
        }
    }

    def getSetting(String paramKey){
        String key = paramKey.toUpperCase().replaceAll("\\.","_")
        return params."${key}"
    }

    void ephemerals(){
        if (!shouldTest()) { return }
        def testNode = Settings.test.node_label
        def envMap = Settings.test.environment
        def testNodeEnv = Helpers.mapToArray(envMap)

        Settings.test.params = [
            tags:          getSetting("test.tags"),
            environment:   getSetting("test.environment"),
            ignore_result: getSetting("test.ignore_result"),
            run_options:   getSetting("test.run_options"),
            mirroring:     getSetting("test.mirroring")
        ]
        Helpers.log.debug("TEST PARAMS:\n" + Helpers.dump(Settings.test))

        stage('Run Test'){
            try {
                if( "virtual".equals(Settings.test.params.environment) ){
                    stageRunVirtual(testNode)
                } else {
                    stageRunExternal(testNodeEnv)
                }
                deploy()
            } catch (Exception ephemeralsException){
                Helpers.log.debug("Something went wrong, skip_errors was set to ${Settings.modules.ephemerals.skip_error}")
                if(!Settings.modules.ephemerals.skip_error){
                    Helpers.error("MAG tests incomplete with errors")
                }
            }
        }

    }

    def deploy(){
        def moduleConfig = Settings.modules.ephemerals
        if( Settings.branch in moduleConfig.deploy ){
            stage('Deploy test'){
                withCredentials([file(
                    credentialsId: 'bot-testing-art-npmrc',
                    variable: 'NPMRC'
                )]){
                    sh 'cp -f ${NPMRC} ~/.npmrc'
                }
                def goal = '-U deploy -Dmaven.test.skip=true -f test/pom.xml'
                Helpers.log.debug("Test environment: ${Settings.test.environment}")
                Modules.maven.goal(goal, Settings.test.environment)
            }
            if ('true'.equals(Settings.test.params.mirroring)) {
            echo 'Mirroring commit'
            build   job: Settings.modules.escenia.mirroring_job,
                    parameters: [
                        string(name: 'MIRRORING_COMMAND', value: 'mirror'),
                        string(name: 'MIRRORING_REPOSITORY', value: env.GIT_URL ?: env.GIT_URL_1),
                        string(name: 'MIRRORING_BRANCH', value: env.BRANCH_NAME)
                    ],
                    wait: true
            }
        }
        return true
    }
}

return this

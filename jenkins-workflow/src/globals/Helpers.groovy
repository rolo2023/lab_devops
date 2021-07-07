package globals
import org.codehaus.groovy.runtime.StackTraceUtils
import static groovy.json.JsonOutput.*
import hudson.model.User
import hudson.model.Cause

class Helpers {

    static def jenkins // WorkflowScript

    static def plugins
    static def access = []

    static final class colors {
        static final String SANE                = "\u001B[0m"
        static final String HIGH                = "\u001B[1m"
        static final String LOW                 = "\u001B[2m"
        static final String ITALIC              = "\u001B[3m"
        static final String UNDERLINE           = "\u001B[4m"
        static final String REVERSE_VIDEO       = "\u001B[7m"
        static final String INVISIBLE_TEXT      = "\u001B[8m"
        static final String BLACK               = "\u001B[30m"
        static final String RED                 = "\u001B[31m"
        static final String GREEN               = "\u001B[32m"
        static final String YELLOW              = "\u001B[33m"
        static final String BLUE                = "\u001B[34m"
        static final String MAGENTA             = "\u001B[35m"
        static final String CYAN                = "\u001B[36m"
        static final String WHITE               = "\u001B[37m"
    }

    static class log {
        final static Integer DEBUG = 3
        final static Integer INFO = 2
        final static Integer WARN = 1
        final static Integer ERROR = 0

        static void info(message){ this.logMsg(INFO, message) }
        static void debug(message){ this.logMsg(DEBUG, message) }
        static void error(message){ this.logMsg(ERROR, message) }
        static void warn(message){ this.logMsg(WARN, message) }
        static void warning(message){ warn(message) }
        static void say(String logLevel, String message) {
            switch (logLevel.toLowerCase()) {
                case 'debug':
                    debug(message)
                    break
                case ['warn', 'warning']:
                    warn(message)
                    break
                case ['err', 'error']:
                    error(message)
                    break
                case 'info':
                    info(message)
                    break
            }
        }

        private static void logMsg(Integer level, Object message){
            String pref
            String color
            switch(level){
                case INFO:
                    pref = '**** INFO ****'
                    color = colors.CYAN
                    break
                case WARN:
                    pref = '**** WARNING ****'
                    color = colors.YELLOW
                    break
                case DEBUG:
                    pref = '**** DEBUG ****'
                    color = colors.MAGENTA
                    break
                case ERROR:
                    pref = '**** ERROR ****'
                    color = colors.RED
                    break
            }
            if (level <= Settings.LOGLEVEL){
                def head = "${colors.HIGH}${color}${pref}${colors.SANE}"
                jenkins.echo "${head} ${color}${message}${colors.SANE}"
            }
        }
    }

    static substituteObject(Object value) {
        if(value in String || value in GString) {
            return subst(value)
        } else if (value in List) {
            return substituteList(value)
        } else if (value in Map) {
            return substituteTree(value)
        } else {
            return value    // Rest of native types: boolean, int, float...
        }
    }

    static List substituteList(List list) {
        List processedList =[]
        list.each { processedList << substituteObject(it) }
        return processedList
    }

    static Map substituteTree(Map tree) {
        Map processedTree = [:]
        tree.each { key, value -> processedTree[key] = substituteObject(value) }
        return processedTree
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    private static String subst(String str=''){
        if (!str || !str.contains('{{') || !str.contains('}}')) {
            return str
        }

        //* Replace all spaces inside, so we can have cleaner vars: {{ arch }}
        str = str.replaceAll(
            /\{\{\s*/, '{{'
        ).replaceAll(
            /\s*\}\}/, '}}'
        )

        //* Some preset replacements, some of them should go to the block below
        str = str.replace(
            '{{verbosity}}', Settings.LOGLEVEL.toString()
        ).replace(
            '{{repo}}', jenkins.env ? (jenkins.env.GIT_URL ?: '') : ''
        )

        //* These probably need to be refactored
        if (Settings.artifact?.file) {
            String name = Settings.artifact.file.name ?: ''
            String path = Settings.artifact.file.path ?: ''

            str = str.replace(
                '{{artifact}}', name
            ).replace(
                '{{release.filePath}}', path
            ).replace(
                '{{release.fileName}}', name
            )
        }

        def jinjaRegex = /(\{\{\s*(\w+)\.?(\w+)?\s*\}\})/
        def m = str =~ jinjaRegex
        while(m.size() > 0) {
            def matches = []; m.each { matches << [ it[1], (it[-1] ? it[2..-1].join('.') : it[2]) ] }
            matches.each {
                def (String jinja, String var) = it
                String v = var.
                        replace('artifact.', '').
                        replace('release.', '').
                        replace('build.', '').
                        replace('env.', '').
                        replace('jenkinsfile.', '').
                        replace('repo.', '').
                        replace('temp.', '').
                        replace('vars.', '').
                        replace('form.', '')
                try {
                    switch(var) {
                        // Deprecated values
                        case ~/^version$/:
                            Helpers.log.warn """
                            {{ version }}, on its own, has been REMOVED.

                            Use {{ repo.version }} for GIT semantic version (if any),
                            or {{ artifact.version }} for the version that will get to Artifactory
                            """
                            str = null
                            return null
                        // Substitutions
                        case ~/^release.*/: // This one falls back to 'artifact', just warn
                            Helpers.log.warn """
                            Using '{{ release }}' will be DEPRECATED soon.
                            Migrate to any of the various {{ artifact }} items ASAP
                            """
                        case ~/^artifact.*/:
                            str = str.replace(jinja, Settings.artifact."${v}") ; break
                        case ~/^env.*/:
                            str = str.replace(jinja, jenkins.env."${v}") ; break
                        case ~/^vars.*/:
                            str = str.replace(jinja, Settings.vars."${v}") ; break
                        case ~/^temp.*/:
                            str = str.replace(jinja, Settings.temp."${v}") ; break
                        case ~/^build.*/:
                            str = str.replace(jinja, Settings.build."${v}") ; break
                        case ~/^repo.pullrequest.*/:
                            str = str.replace(jinja, Settings.repo.pullrequest."${v}") ; break
                        case ~/^repo.*/:
                            str = str.replace(jinja, Settings.repo."${v}") ; break
                        case ~/^form.*/:
                            // Type coercion is needed here, booleans can be set and replace(String, Boolean) is illegal
                            String formValue = jenkins.params."${v}"
                            if (!formValue || formValue == "null") {
                                formValue = Settings.formValues.hiddenParams."${v}"
                            }
                            str = str.replace(jinja, formValue) ; break
                        case ~/^jenkinsfile.*/:
                            str = str.replace(jinja, Settings.jenkinsfile."${v}") ; break
                        default:
                            str = str.replace(jinja, Settings."${var}") ; break
                    }
                } catch (NullPointerException npe) {
                    Helpers.log.warn "Trying to replace ${jinja} with null value"
                    str = null
                    return null
                } catch (Exception replaceException){
                    Helpers.log.warn "Could not find any valid 'property' ${var} to use in ${str.trim()}: ${replaceException}"
                    str = null
                    return null
                }
            }
            m = str =~ jinjaRegex
        }
        if (str == null) {
            Helpers.error "Could not substitute all variables"
            return null
        }
        return str.trim()
    }

    static Long now(withMiliseconds=false){
        return dateToUnix(null, withMiliseconds)
    }

    static Long dateToUnix(date=null, withMiliseconds=false, hasMiliseconds=true){
        def dt = new Date()
        def dateFmt = hasMiliseconds ? 'yyyy-MM-dd hh:mm:ss.SSS' : 'yyyy-MM-dd hh:mm:ss'
        if(date){ dt = dt.parse(dateFmt, date) }
        def t = dt.getTime()
        if(withMiliseconds == false){ t = t / 1000 }
        return t as Long
    }

    static List mapToArray(Map map){
        def array = []
        map.each { k, v ->
            def value = Helpers.subst(v.toString())
            if(!(value in ['', null])){
                array << "${k}=${value}"
            }
        }
        return array
    }

    static Map stringToMap(mapAsString){
        return mapAsString[1..-2].split(', ').collectEntries {
                    def pair = it.split(':')
                    [(pair.first()): pair.last()]
                }
    }

    static void run(Map step, Map stageEnv=[:]) {

        if(!Helpers.canDoWhen(step)) return

        Helpers.log.warn """
[DEPRECATION NOTICE]

The use of the 'run' module is not recommended, as it executes random code that just exists in your configuration.
This makes pipelines difficult to test and may lead to inconsistent runs.

Consider migrating to the various modules found in the 'vars/' directory, and if the functionality is not there,
that is an issue we need to discuss, plan and implement together.
"""

        def envMap = this.getEnvMap(stageEnv)
        def envCmd = this.mapToArray(envMap)
        def runCmd = this.subst(step.run)
        def runDir = this.jenkins.workspace
        step.environment.each { k, v ->
            def value = Helpers.subst(v)
            if(k.toLowerCase() == 'workspace') {
                runDir = value
            } else {
                envMap."${k}" = value
                envCmd << "${k}=${value}"
            }
        }

        def stageLabel = step.label ?: "run ${step.bin}"
        Helpers.log.debug "RUN ${stageLabel}\n\tCommand: ${step.bin} ${runCmd}\n\tEnvironment (before processing):\n\t\t${envCmd.join('\n\t\t')}"
        jenkins.dir(runDir) {
            jenkins.withEnv(envCmd) {
                switch(step.bin) {
                    case 'mvn':
                        Helpers.log.warn "[DEPRECATION NOTICE] Instead of using 'bin', migrate to the 'maven' module."
                        step << [ environment: envMap]
                        jenkins.maven.fromRunCommand(step)
                        break
                    case 'sh':
                        this.runSh (runCmd, step.resource, step.credentials)
                        break
                    case 'groovy':
                        Eval.xyz(jenkins, Settings, this, step.run)
                        break
                }
            }
        }
    }

    static void runSh(String command, String resourceFile, List credentials){
        def shCmd = resourceFile ? jenkins.libraryResource(resourceFile) : command
        if (credentials == null || credentials.size() == 0) {
            Helpers.log.debug "Helpers :: runSh :: executing ${shCmd} without credentials"
            jenkins.sh shCmd
        } else {
            def wCredentials = []
            credentials.each { credential ->
                Helpers.log.debug("HELPERS :: runSH :: Add credential ${credential.name} of type ${credential.type} to var ${credential.variable}")
                wCredentials << getCredential(credential.type, credential.name, credential.variable)
            }
            jenkins.withCredentials(wCredentials) { jenkins.sh shCmd }
        }
    }

    static Map loadResource(String resource, String ext=null){
        if(!ext) ext = resource.split('\\.').last()
        try {
            def contents = jenkins.libraryResource(resource)
            switch(ext){
                case 'yml':
                    return jenkins.readYaml (text: contents )
                    break
                default:
                    return contents
                    break
            }
        } catch (Exception fileNotFoundException){
            return [:]
        }
    }

    static void node(String name, Closure c) {
        jenkins.node(name) {
            c()
        }
    }

    static void cleanNode(){
        try {
            // This might not be installed, although it is a recommended plugin
            jenkins.cleanWs()
        } catch (ignored) {
            Helpers.log.warn "cleanWorkspace plugin not installed, not properly configured: manually deleting"
            jenkins.sh "rm -fR ${jenkins.workspace}/* ${jenkins.workspace}/.[^.]*"
        }
    }

    static String dump(List list){
        String sep = '@-#-@'
        return '   ' + list.join(sep).replace('\\n', '\n').replace(sep, '\n   ')
    }

    // This method is usually used to debug, no way crashing pipelines because of it
    static String dump(Map map){
        try {
            return prettyPrint(toJson(map))
        } catch (jsonException) {
            return "${map}"
        }
    }

    static String getMapValueByPath(map, path, defaultValue = null) {
        Map item = map
        path.split('\\.').each {
            item = item.get(it, [:])
        }
        return item ?: defaultValue
    }

    static void error(String message){
        log.error message
        jenkins.error "---------"
    }

    static void error(Exception e){
        try {
            StackTraceUtils.sanitize(e)
            def trace = e.getStackTrace().join('\n[TRACE] ')
            def message = e.toString()
            error "${message}\nSTACK TRACE BEGIN *v*v*v*\n[TRACE] ${trace}\nSTACK TRACE END *^*^*^*"
        } catch (ignored) {
            error "${e.toString()}"
        }

    }

    static def getCredential(String credentialType, String rawCredentialsId, String varName){
        def credentialsId = subst(rawCredentialsId)
        switch (credentialType){
            case [ 'usernameColonPassword', 'userpass', 'curl' ]:
                return jenkins.usernameColonPassword(credentialsId: credentialsId, variable: varName)
                break
            case [ 'usernamePassword', 'userSplitPassword' ]:
                return jenkins.usernamePassword(credentialsId: credentialsId, usernameVariable: "${varName}_USR", passwordVariable: "${varName}_PSW")
                break
            case [ 'string', 'text' ]:
                return jenkins.string(credentialsId: credentialsId, variable: varName)
                break
            case [ 'file' ]:
                try{
                    return jenkins.file(credentialsId: credentialsId, variable: varName)
                }catch(NullPointerException e){
                    return [$class: 'FileBinding', credentialsId: credentialsId, variable: varName ]
                }
                break
            case [ 'sshUserPrivateKey', 'rsa' ]:
                return jenkins.sshUserPrivateKey(credentialsId: credentialsId, keyFileVariable: varName)
                break
            default:
                error "Helpers :: getCredentials :: Unknown/Unsupported credential type: ${credentialType}"
        }
    }

    static Boolean credentialExists(String credentialType, String credentialId) {
        Helpers.credentialExists "${credentialType}: ${credentialId}"
    }

    /**
     * Receives an item in the form 'cred_type: raw_credential_id', and returns
     * true if a valid Jenkins Credential exist for the expected type.
     *
     * We do not check for 'env' type, since they depend on the module they are called from
     *
     * @param credentialId
     * @return
     */
    static Boolean credentialExists(String rawCredential){

        // Do not fail on empty credentials
        if (!rawCredential) { return true }

        // Do not even check badly substituted variables
        if (rawCredential.contains('{{') || rawCredential.contains('}}')) {
            error "Helpers :: credentialExists :: Checking templated credential '${rawCredential}'"
            return false
        }

        log.debug "Helpers  :: credentialExists :: looking for '${rawCredential}'"

        def (String credType, String rawId) = rawCredential.split(':').collect { it.trim() }
        if (credType == 'env') {
            log.warn "Helpers  :: credentialExists :: don't know how to check credential of type '${credType}"
            return true
        }

        // Maybe credential comes without explicit type? check all
        if (credType && !rawId) {
            return ['userpass', 'string', 'file', 'rsa'].any { credentialLookup(it, credType) }
        } else {
            return credentialLookup(credType, rawId)
        }
    }

    private static Boolean credentialLookup(String type, String id) {
        try {
            def credential = getCredential(type, id, 'fake')
            if (!credential) { return false }
            jenkins.withCredentials([credential]) { return true }
        } catch (org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException _) {
            return false
        } catch (any) {
            // If it's something other than 'CredentialNotFoundException' we need to know
            log.warn "Helpers :: credentialLookup :: Error retrieveing credential ${id} of type '${type}' :: ${any}"
            return false
        }
    }

    static List recursiveKey(Map map, Object key){
        List found = []
        map.keySet().each { k ->
            def v = map[k]
            if(k == key){
                found << v
            } else if(v in Map){
                found << recursiveKey(v, key)
            }
        }
        return (found.flatten() as List).unique()
    }

    // Obtiene el mapa de variables Settings.environment + Settings.STAGE.environment + customEnv
    private static Map getEnvMap(Map customEnv=[:]){
        Map envMap = Settings.environment ?: [:]

        try {
            Map stageSettings = Settings."${Settings.currentStage}".environment ?: [:]
            envMap << stageSettings
        } catch (anything) {}

        if (customEnv != null) envMap << customEnv
        return envMap
    }
    static loadLibrary(String library, String version) { return loadLibrary("${library}@${version}") }
    static loadLibrary(String libraryAndVersion) {
        Helpers.log.debug "Helpers :: loadLibrary :: loading ${libraryAndVersion}"
        return jenkins.library(libraryAndVersion)
    }

    /**
     * Loads a shared library directly, without the need of configuring it in Jenkins
     * @link https://jenkins.io/doc/book/pipeline/shared-libraries/#retrieval-method
     *
     * WARNING:
     * The library will be UNTRUSTED, and there's no way around it
     *
     * @param libraryGitRepo    ssh url to the git repo to clone
     * @param id                ID that will be given to that repo locally
     * @param version           branch / version to checkout
     * @return  a library object
     */
    static loadLibraryDirectly(String libraryGitRepo, String id, String version = 'master') {
        log.debug("Loading ${id}@${version} from ${libraryGitRepo}... ")

        // We use the Ether credentials here, they should have access to anything
        def libraryRetriever = jenkins.modernSCM([
                $class: 'GitSCMSource',
                remote: libraryGitRepo,
                credentialsId: Settings.ether.git.credentialsId
        ])
        return jenkins.library(
                identifier: "${id}@${version}",
                retriever: libraryRetriever)
    }

    static List getConditions(Map stepMap){
        List conditions = []
        List conditionKeys = stepMap.keySet() as List
        if ('when' in conditionKeys){
            if (stepMap.when instanceof List){
                conditions = stepMap.when
            } else if(stepMap.when instanceof Map){
                conditions << stepMap.when
            } else {
                // Backwards compatibility with old "when" statement
                conditions << [ true: stepMap.when ]
            }
        }
        // Backwards compatibility with "when_not" and "when_branch" statements
        if ('when_branch' in conditionKeys) {
            conditions << [branch: stepMap.when_branch]
        }
        if ('when_not' in conditionKeys) {
            conditions << [false: stepMap.when_not]
        }
        return conditions
    }

    //* Overload, in case a bare string come we assume it's meant to be true
    static Boolean canDoWhenEach(String condition) {
        canDoWhenEach([true: condition])
    }

    static Boolean canDoWhenEach(Map condition) {
        def key = condition.keySet()[0]
        def value = substituteObject(condition.get(key))
        switch(value.toString()){
            case ['yes', 'always', 'true']:
                value = "true"
                break
            case ['no', 'never', 'false']:
                value = "false"
                break
        }
        Boolean can
        switch(key.toString()){
            case ['true', 'false']:
                can = (Eval.x(jenkins, value) as Boolean).toString() == key.toString()
                break
            case 'branch':
                can = value.any { branch -> Settings.repo.branch  =~ /${branch}/ }
                break
            case 'found':
                can = jenkins.fileExists(value) as Boolean
                break
            case 'not_found':
                can = !(jenkins.fileExists(value) as Boolean)
                break
            default:
                log.warn("Unknown condition '${key}'. Will return FALSE")
                can = false
                break
        }
        return can
    }

    static Boolean canDoWhen(List conditions) {
        // Evaluate all conditions, if any is False, we cannot accept
        Boolean cannot = conditions.any { !canDoWhenEach(it) }
        log.debug "Helpers :: canDoWhen :: ${conditions} is ${!cannot}"

        return !cannot
    }
    static Boolean canDoWhen(Map stepMap) {
        List conditions = getConditions(stepMap)
        this.canDoWhen(conditions)
    }

    static Map colon(String colonStr){
        Map mColon = [ key: 'string', value: colonStr ]
        if (!colonStr) return mColon

        List lColon = colonStr.split(':')
        if (lColon.size() > 1) mColon << [ key: lColon[0].trim(), value: lColon[1..-1].join(':').trim() ]
        return mColon
    }

    static Boolean pluginExists(String pluginId) {
        return this.plugins.findAll { plugin ->
            plugin.getShortName() == pluginId
        }.size() == 1
    }
    /**
     * We can use this to return if a user belongs or not to a group of GRaaS
     * Example:
     * <code>
     *      if ( Helpers.isCurrentUserInGraasGroup('groupGraaS') ){
     *           Helpers.log.info "You have access for deploy"
     *      }
     *      else{
     *           exit "CheckPermissions :: You don´t have access for deploy"
     *      }
     * </code>
     * @param accessUserGrassStr Group to verify with the users groups that trigger the current job
     */
    static Boolean isCurrentUserInGraasGroup(String accessUserGrassStr) {
           if(access == []){ access = User.get(jenkins.currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId(), false).getAuthorities() }
           return accessUserGrassStr in access
    }

    static void sendEmail(Map params=[:]){
        this.jenkins.emailext subject: params.subject, body: params.body, to: params.mails
    }

    /**
     * Blocks execution for 'waitMinutes' minutes or until we get an approval from one of the users
     * in the 'emailApprovals' list.
     */
    static Boolean approval(String emailsApprovals,
            String subjectEmail='Despliegue Automatico Devops',
            String bodyEmail='<p>Para aprobar el despliegue hacer clic&nbsp;<a href="{ env.BUILD_URL }input/">aquí</a>.</p>',
            int waitMinutes=10 //MINUTES
        ) {

        def paramsEmail = [ subject: subjectEmail, body: bodyEmail, mails: emailsApprovals ]
        Helpers.sendEmail(paramsEmail)

        def users = emailsApprovals.replace((emailsApprovals.split(',')[0] =~ /(@[A-Za-z0-9-]{1,63}.*$)/)[0][1],'')
        def userInput = null
        try {
           Helpers.log.info("Esperando aprobación de ${emailsApprovals} durante ${waitMinutes} minutos...")
           this.jenkins.timeout(time: waitMinutes, unit: 'MINUTES') {
                userInput = this.jenkins.input submitterParameter: 'approval', submitter: "${users}", message: "${subjectEmail}", parameters: [ [$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'Aprobar'] ]
            }
        } catch(err) {
            Helpers.log.error "Approval :: Se supero el tiempo de espera del usuario ${users}"
            Helpers.log.debug("Approval :: Error -> ${err}")
            return false
        }

        if(userInput != null && userInput.Aprobar == true) {
            Helpers.log.info "Approval :: Aprobado por ${userInput.approval}"
            return true
        } else {
            Helpers.log.warn "Approval :: RECHAZADO por ${userInput.approval}"
            return false
        }
    }

    /**
     * Retrieve an artifact for the deployment, either from a stashed ID or from Artifactory.
     *
     * If it comes from Artifactory, we assume the Artifactory module credentials work fine.
     *
     * @return true if artifact was unstashed, false if it was downloaded
     */
    static Boolean getArtifacts(String localFileName, String remoteFileUrl=null){
        def applyUnstash = remoteFileUrl ? false : true
        if (applyUnstash){
            Helpers.log.debug("Helpers :: getArtifacts :: downloading with unstash")
            jenkins.unstash(localFileName)
        } else {
            Helpers.log.debug("Helpers :: getArtifacts :: downloading with curl")
            jenkins.curl([
                action: 'GET',
                url: remoteFileUrl,
                localFile: localFileName,
                credentials: Settings.modules.artifactory.credentialsId
            ])
        }
        return applyUnstash
    }
}

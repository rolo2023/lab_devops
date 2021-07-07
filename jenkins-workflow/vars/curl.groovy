import globals.Helpers

/**
 * CURL wrapper
 *
 * Mandatory parameters:
 * - url
 * - action
 * Optional parameters:
 * - credentials: 'username:password' used to identify curl via HTTP headers
 * - proxy: 'proxyURL' is used to select a proxy for the HTTP connectio
 * - headers: headers in list form (eg: [ 'Accept: text/json' ])
 *
 * Mandatory params for PUT
 * - localFile: path to local file where remote will be copied to
 */
def call(String action, String url){ return call (action:action, url:url) }

def call(Map config) {
    fromMap(config).checkMandatory(['action', 'url'])

    String action = config.action.toLowerCase()
    int statusCode = -1

    // Get action for curl before anything
    String actionCmd = ''
    try {
         actionCmd = this."x${action}"(config)
    } catch (MissingMethodException ex) {
        error "Unknown or unavailable action '${action}'"
    }

    List credentialList = []
    if(config.credentials) credentialList << usernameColonPassword(credentialsId: config.credentials, variable: 'CURL')

    List cmd = [ "curl --fail" ]
    if(!config.verbose) cmd << '-s'
    if(config.proxy) cmd << "-x '${config.proxy}'"
    cmd << getHeaders(config.headers)

    // Run curl
    withCredentials(credentialList){
        if(config.credentials) cmd << "-u '${CURL}'"
        cmd << actionCmd
        statusCode = sh (returnStatus: true, script: "${cmd.join(' ').trim()}")
    }

    if(statusCode != 0) Helpers.error "curl exited with status ${statusCode}. See 'https://curl.haxx.se/libcurl/c/libcurl-errors.html' for more information about curl exit codes"
}

def fromStep(Map args){
    Map curlCfg = [
        action: args.action,
        url: args.url,
        localFile: args.localFile,
        credentials: Helpers.colon(args.credentials).value,
        headers: args.headers
    ]
    call curlCfg
}

private String getHeaders(config){
    List headers = []
    List cfgHeaders = config ?: []
    cfgHeaders.each { headers << "-H ${it}" }
    return headers.join(' ')
}

private String xget(config) {
    List cmd = [ "-XGET '${config.url}'" ]
    if(config.localFile) cmd << "-o '${config.localFile}'"
    return cmd.join(' ')
}

private String xput(config) {
    if (!config.localFile) { error 'A localFile value is needed in order to use PUT' }
    return "-XPUT '${config.url}' -T '${config.localFile}'"
}

return this

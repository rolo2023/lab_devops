package api.bitbucket

import api.requests.Headers
import api.requests.Session


class Bitbucket implements Serializable {

    private String url
    private Session session
    private List auth
    private Script jenkins

    static class AuthenticationException extends Exception {}
    static class PrAlreadyExistsException extends Exception {}

    static def response(Map result){
        if (result.ok) return result
        def errMsg = result.error.message.errors[0]
        def exceptionMsg = errMsg.message.toString()
        switch(errMsg.exceptionName.toString()){
            case 'com.atlassian.bitbucket.AuthorisationException':
                throw new AuthenticationException()
                break
            case 'com.atlassian.bitbucket.pull.DuplicatePullRequestException':
                throw new PrAlreadyExistsException()
                break
            default:
                throw new Exception(exceptionMsg)
                break
        }
    }

    Bitbucket(jenkins, url, authToken, authPassword=null, encryptedAuth=false){
        this.url = "${url}/rest/api/1.0"
        String authHeader = null
        // Possible auth header settings:
        // authPassword is set, so authToken is indeed authUsername (encryptedAuth = ${whatever})
        // authToken is REALLY a token and is already encryped (encryptedAuth = true)
        // authToken is REALLY a token and is not encrypted (encryptedAuth = false)
        if(authPassword) {
            String encryptedPassword = Headers.encode("${authToken}:${authPassword}")
            authHeader = "Basic ${encryptedPassword}"
        } else if(encryptedAuth) {
            authHeader = "Basic ${authToken}"
        } else {
            authHeader = "Bearer ${authToken}"
        }
        Map headers = Headers.JSON + ['Authorization': authHeader, 'X-Atlassian-Token': 'nocheck' ]
        this.session = new Session(baseUrl: this.url, headers: headers)
        this.jenkins = jenkins
    }

    Project projects(String key=null){
        return new Project(this.jenkins, this.session, key)
    }

}
package modules

import api.bitbucket.Bitbucket
import globals.*
import jenkins.Pipeline

import org.apache.commons.codec.binary.Base64

static BitbucketModuleCps newCps(){ new BitbucketModuleCps() }

/**
 * Bitbucket API wrapper, used to get access to PR information,
 *  and to create a PR if so is configured.
 **/
class BitbucketModuleCps extends Pipeline {

    private static String encode(String text){
        Base64.encodeBase64String(text.getBytes())
    }

    private def client = null

    def getBitbucketClient() {
        if (this.client != null) {
            return this.client
        }

        Map moduleConfig = Helpers.substituteTree(Settings.modules.bitbucket)
        String credToken = null
        String credPassword = null
        Boolean credTokenEncrypted = false
        String apiTokenId = moduleConfig.credentialsId
        switch(moduleConfig.credential_type){
            //* The API constructor will turn these into a  base64-encoded username:password Bearer Token
            case 'userpass':
                withCredentials([usernamePassword(
                                                    credentialsId: apiTokenId,
                                                    usernameVariable: 'BBUSERNAME',
                                                    passwordVariable: 'BBPASSWORD'
                                                )]
                ){
                    credToken = encode("${ BBUSERNAME.trim() }:${ BBPASSWORD.trim() }")
                    credTokenEncrypted = true
                }

                break
            //* This would be a base64-encoded username:password Bearer Token
            case 'string_base64':
                credTokenEncrypted = true
                withCredentials([string(credentialsId: apiTokenId, variable: 'AUTH')]){ credToken = "${ AUTH.trim() }" }
                break
            default: // usually 'string'
                withCredentials([string(credentialsId: apiTokenId, variable: 'AUTH')]){ credToken = "${ AUTH.trim() }" }
                break
        }
        this.client = new Bitbucket(this, moduleConfig.url, credToken, credPassword, credTokenEncrypted)
        this.client
    }

    void init(){
        this.client = this.getBitbucketClient()
    }

    Map getPrInfo(prId){
        def gitCfg = Settings.repo
        def pullRequest = this.getBitbucketClient().projects(gitCfg.project).repos(gitCfg.slug).pullrequests(prId).get()
        Map prInfo = [ fromRef: pullRequest.getFromRef(), toRef: pullRequest.getToRef() ]
        Helpers.log.debug("Modules :: Bitbucket :: Info for PR #${prId}: ${prInfo}")
        return prInfo
    }

    void createPr(){
        List targets = []
        switch(Settings.repo.pullrequest.comesFrom){
            // Eventually, hotfixes will go to release as well
            case ~/hotfix.*/:
                targets = [ 'develop' ]
                break
            case ~/bugfix.*/:
                targets = [ 'develop' ]
                break
        }

        def gitCfg = Settings.repo
        def pr = this.getBitbucketClient().projects(gitCfg.project).repos(gitCfg.slug).pullrequests()
        targets.each { target ->
            try {
                pr.create(
                    Settings.repo.pullrequest.comesFrom,
                    target,
                    'Automatic Pull Request by Jenkins workflow'
                )
            } catch (Exception ex){
                if (ex.toString() == 'api.bitbucket.Bitbucket$PrAlreadyExistsException'){
                    Helpers.log.warn('PR already existed')
                } else {
                    Helpers.error ex
                }
            }
        }
    }
}

return this

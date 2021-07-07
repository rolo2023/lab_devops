#!groovy
package api.vtrack.v0

import api.requests.RestJsonClient
import api.vtrack.IBuild
import api.vtrack.IDeploy

class Build implements Serializable, IBuild {
    Script script
    RestJsonClient session
    Map values
    List deploys
    String id
    String baseUrl

    Build(Script script, RestJsonClient session, String componentUrl, Map values){
        this.session = session
        this.script = script
        this.values = values
        this.id = values['_id']
        this.baseUrl = String.format(Routes.builds, componentUrl) + "/${this.id}"
        this.deploys = []
    }

    Map json(){
        return this.values
    }

    void exception(String task, String message){
        this.script.echo "*** Unable to perform task ${task}.\n*** Server response: ${message}\n***"
        throw new Exception("VtrackErrorOn${task.capitalize()}")
    }

    IDeploy createDeploy(Map deployMap){
        String url = "${this.baseUrl}/deploys"
        Map response = this.session.post(url, deployMap)
        if (response.ok && response.data){
            IDeploy deploy = new Deploy(this.script, this.session, this.baseUrl, response.data)
            this.deploys << deploy
            return deploy
        } else {
            String msg = response.error ?: response.toString()
            exception('createDeploy', msg)
        }
    }

    Map generateDeployMap(Map deployMap=[:]){
        Map dMap = [
            status : 'deployed',
            params : '',
            environment : 'dev',    // dafe default
            country : 'ESP',
            userdeploy : 'nouser@nomail',
            date : false
        ]
        dMap << deployMap
        return dMap
    }

}

#!groovy
package api.vtrack.v0

import api.requests.RestJsonClient
import api.vtrack.IDeploy

class Deploy implements Serializable, IDeploy {
    Script script
    RestJsonClient session
    Map values
    String id
    String baseUrl

    Deploy(Script script, RestJsonClient session, String buildUrl, Map values){
        this.session = session
        this.script = script
        this.id = values['_id']
        this.baseUrl = String.format(Routes.deploys, buildUrl) + "/${this.id}"
        this.values = values
    }

    Map json(){
        return this.values
    }

    void exception(String task, String message){
        this.script.echo "*** Unable to perform task ${task}.\n*** Server response: ${message}\n***"
        throw new Exception("VtrackErrorOn${task.capitalize()}")
    }

}

#!groovy
package api.vtrack.v0

import api.requests.RestJsonClient
import api.vtrack.IBuild
import api.vtrack.IComponent


class Component implements Serializable, IComponent {
    Script script
    RestJsonClient session
    Map values
    List builds
    String namespace
    String id
    String baseUrl

    Component(Script script, RestJsonClient session, String namespace, Map values) {
        this.session = session
        this.script = script
        this.namespace = namespace
        this.values = values
        this.id = values['_id']
        this.baseUrl = String.format(Routes.components, namespace) + "/${this.id}"
        this.builds = []
    }

    Map json(){
        return this.values
    }

    void exception(String task, String message){
        this.script.echo "*** Unable to perform task ${task}.\n*** Server response: ${message}\n***"
        throw new Exception("VtrackErrorOn${task.capitalize()}")
    }

    IBuild[] getComponentBuilds(int pageSize=10){
        String buildsUrl = String.format(Routes.builds, this.baseUrl)
        String url = buildsUrl + "?pageSize=" + pageSize.toString()
        Map response = this.session.get(url)
        if (response.ok && response.data){
            for(buildItem in response.data){
                IBuild build = new Build(this.script, this.session, this.baseUrl, buildItem)
                this.builds << build
            }
            return this.builds
        } else {
            String msg = response.error ?: response.toString()
            exception('getComponentBuilds', msg)
        }
    }

    IBuild getComponentBuild(String buildId){
        String buildsUrl = String.format(Routes.builds, this.baseUrl)
        String url = buildsUrl + "/" + buildId
        Map response = this.session.get(url)
        if (response.ok && response.data){
            IBuild build = new Build(this.script, this.session, this.baseUrl, response.data)
            return build
        } else {
            String msg = response.error ?: response.toString()
            exception('getComponentBuild', msg)
        }
    }

    IBuild createBuild(Map buildMap){
        String url = String.format(Routes.builds, this.baseUrl)
        Map response = this.session.post(url, buildMap)
        if (response.ok && response.data){
            IBuild build = new Build(this.script, this.session, this.baseUrl, response.data)
            return build
        } else {
            String msg = response.error ?: response.toString()
            exception('createBuild', msg)
        }
    }

    Map generateBuildMap(Map buildMap=[:]){
        def env = this.script.env
        Map bMap = [
            url: env.BUILD_URL,
            slave: false,
            status: 'failed',   // do not set successful by default
            version: env.BUILD_NUMBER,
            userbuild: 'nouser@nomail',
            commit: env.GIT_COMMIT ?: '',
            start_date: System.currentTimeMillis(), //Needs in-script approval
            end_date: System.currentTimeMillis(), //Needs in-script approval
            branch: env.BRANCH_NAME,
            buildNumber: env.BUILD_NUMBER,
            metadata : [:],
            artifacts: []
        ]
        bMap << buildMap
        return bMap
    }

}

#!groovy
package api.vtrack.v0

import api.requests.RestJsonClient
import api.vtrack.IComponent
import api.vtrack.INamespace

class Namespace implements Serializable, INamespace {
    Script script
    RestJsonClient session
    Map values
    String id

    Namespace(Script script, RestJsonClient session, Map values){
        this.session = session
        this.id = values['_id']
        this.script = script
        this.values = values
    }

    Map json(){
        return this.values
    }

    void exception(String task, String message){
        throw new Exception("VtrackErrorOn${task.capitalize()}")
    }

    Map generateComponentMap(Map componentMap=[:]){
        if(componentMap.uuaa && componentMap.uuaa.size() > 4) componentMap.uuaa = componentMap.uuaa[0..3]
        Map compMap = [
            component: '',
            application: '',
            project: 'TEST',
            architecture: '',
            vcs_repo: this.script.env.GIT_URL,
            uuaa: 'TEST',
            package_type: ''
        ]
        compMap << componentMap
        return compMap
    }

    IComponent insertComponent(Map componentMap){
        String url = String.format(Routes.components, this.id)
        Map response = this.session.post(url, componentMap)
        if (response.ok && response.data){
            IComponent component = new Component(this.script, this.session, this.id, response.data)
            return component
        } else {
            this.script.echo "*** DEBUG: ${response} ***"
            String msg = response.error ?: response.toString()
            exception('createComponent', msg)
        }
    }

    IComponent getComponent(String name, Map componentToCreateIfNone=null){
        String url = String.format(Routes.components, this.id) + "/${name}"
        Map response = this.session.get(url)
        if(response.ok && response.data){
            IComponent component = new Component(this.script, this.session, this.id, response.data)
            return component
        } else if(componentToCreateIfNone){
            return insertComponent(componentToCreateIfNone)
        } else {
            return null
        }
    }

    IComponent[] getComponents(Map andRsql){
        String url = String.format(Routes.components, this.id)
        andRsql.eachWithIndex { key, value, index ->
            if(index == 0){
                url = url + '?q='
            }
            url = String.format(url + '%s=="%s"', key, value)
            if(index < andRsql.size()-1){
                url = url + ';'
            }
        }
        Map response = this.session.get(url)
        if(response.ok && response.data){
            List components = []
            for(component in response.data){
                IComponent c = new Component(this.script, this.session, this.id, component)
                components.add(c)
            }
            return components
        }
        return null
    }

    IComponent[] getComponents(String rawSql){
        String url = String.format(Routes.components, this.id)
        url = url + '?q=' + rawSql
        Map response = this.session.get(url)
        if(response.ok && response.data){
            List components = []
            for(component in response.data){
                IComponent c = new Component(this.script, this.session, this.id, component)
                components.add(c)
            }
            return components
        }
        return null
    }

    IComponent[] getComponents(){
        String url = String.format(Routes.components, this.id)
        Map response = this.session.get(url)
        if(response.ok && response.data){
            List components = []
            for(component in response.data){
                IComponent c = new Component(this.script, this.session, this.id, component)
                components.add(c)
            }
            return components
        }
        return null
    }

}

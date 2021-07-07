package api.bitbucket

import api.requests.Session

class PullRequest implements Serializable {

    private def jenkins
    private Session session
    private String context
    private String key
    private Map parents

    private Map data


    PullRequest(jenkins, session, parents, key=null){
        this.jenkins = jenkins
        this.session = session
        this.context = "projects/${parents.project}/repos/${parents.slug}/pull-requests"
        this.key = key
        this.parents = parents
    }

    private String shortRef(String ref){
        return ref.replace('refs/heads/', '').replace('refs/tags/', '')
    }

    String toString(){
        return this.data.toString()
    }

    PullRequest get(){
        if(this.key){
            String url = "${this.context}/${this.key}"
            def result = this.session.get(url)
            this.data = Bitbucket.response(result)
            return this
        }
    }

    // Create Pull Request with basic fields
    PullRequest create(String fromBranch, String toBranch, String title){
        return this.create([
            title: title,
            fromRef: [
                id: "refs/heads/${fromBranch}",
                repository: [ slug: this.parents.slug, project: [ key: this.parents.project ] ],
            ],
            toRef: [
                id: "refs/heads/${toBranch}",
                repository: [ slug: this.parents.slug, project: [ key: this.parents.project ] ],
            ]
        ])
    }

    // Create Pull Request with complete fields (see Bitbucket API)
    PullRequest create(Map data){
        Map response = this.session.post(this.context, data)
        this.data = Bitbucket.response(response)
        return this
    }

    String getFromRef(shortRef=true){
        if(shortRef){
            return this.shortRef(this.data.fromRef.id)
        } else {
            return this.data.fromRef.id
        }
    }

    String getToRef(shortRef=true){
        if(shortRef){
            return this.shortRef(this.data.toRef.id)
        } else {
            return this.data.toRef.id
        }
    }

    String getTitle(){ return this.data.title }
    String getDescription(){ return this.data.description }

    String comesFrom(){ return getFromRef(true) }
    String goesTo(){ return getToRef(true) }
    Map asMap(){ return this.data }
}
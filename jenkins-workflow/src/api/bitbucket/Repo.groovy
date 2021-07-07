package api.bitbucket

import api.requests.Session

class Repo implements Serializable {

    private Session session
    private String context
    private String slug
    private def jenkins
    private Map parents
    private Map data

    Repo(jenkins, session, parents, slug){
        this.context = "projects/${parents.project}/repos"
        this.session = session
        this.jenkins = jenkins
        this.slug = slug
        this.parents = parents + [ slug: slug ]
    }

    Map get(){
        def url = "${this.context}/${this.slug}"
        this.data = Bitbucket.response(this.session.get(url))
        return this
    }

    PullRequest pullrequests(String key=null){
        return new PullRequest(this.jenkins, this.session, this.parents, key)
    }

    private List refs(ref){
        def url ="${this.context}/${this.slug}/${ref}"
        
        def response = Bitbucket.response(this.session.get(url))
        if(response.ok){
            List repoRefs = []
            response.values.each { repoRefs << it.displayId }
            return repoRefs
        }
    }

    List tags(){ return this.refs('tags') }
    List branches(){ return this.refs('branches') }
    Map asMap(){ return this.data }

}

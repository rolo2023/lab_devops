package api.bitbucket

import api.requests.Session

class Project implements Serializable {

    private Session session
    private def jenkins
    private String key
    private static final String context = 'projects'
    private Map parents
    private Map data

    Project(jenkins, session, key){
        this.jenkins = jenkins
        this.key = key       
        this.session = session
        this.parents = [ project: this.key ]
    }

    def get(){
        def url = "${context}/${this.key}"
        Map result = this.session.get(url)
        this.data = Bitbucket.response(result)
        return this
    }

    def repos(String slug=null){
        return new Repo(this.jenkins, this.session, this.parents, slug)
    }

    Map asMap(){ return this.data }

}
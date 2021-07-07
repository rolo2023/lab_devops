#!groovy
package api.requests

import groovy.json.JsonOutput

class RestJsonClient implements Serializable {
  def script
  def headers
  def baseUrl
  def debug
  def etherCredentialsPath = "/tmp/${ System.currentTimeMillis() }"
  def credentialFileId
  def keyFileId

  RestJsonClient(script, args) {
    def baseUrl = args.get('baseUrl', null)
    if(!baseUrl){
      throw new Exception ('baseUrl cannot be blank')
    }

    def headers = [
      "-H 'Content-Type: application/json'",
      "-H 'Accept: application/json'"
      ]
    if(args.proxy){ headers << "--proxy '${args.proxy}'" }

    this.script = script
    this.baseUrl = baseUrl
    this.headers = headers
    this.debug = args.debug ?: false

    // Store the Jenkins credential IDs for Ether
    this.credentialFileId = args.get('credentialFileId', null)
    this.keyFileId = args.get('keyFileId', null)
    if (this.keyFileId == null || this.credentialFileId == null) {
      throw new Exception ('Ether bot credentials not found: ${args}')
    }
  }

  private void createEtherCredentials() {
      this.script.withCredentials([
        this.script.file(credentialsId: this.credentialFileId, variable: 'etherCertificateSecretFileText'),
        this.script.file(credentialsId: this.keyFileId, variable: 'etherPrivateKeyText')
      ]) {
        this.script.sh(script: """
            { set +x; } 2>/dev/null
            if [ ! -d ${this.etherCredentialsPath} ] ; then
                mkdir -p ${this.etherCredentialsPath}
                cp ${this.script.etherCertificateSecretFileText} ${this.getEtherCertificate()}
                cp ${this.script.etherPrivateKeyText} ${this.getEtherPrivateKey()}
            fi
        """)
    }
  }

  private String getEtherCredentialPath(String name) {
      "${this.etherCredentialsPath}/${name}"
  }

  private String getEtherCertificate() {
      return this.getEtherCredentialPath('cert.pem')
  }

  private String getEtherPrivateKey() {
      return this.getEtherCredentialPath('key.pem')
  }

  def compose(context){
    def url = this.baseUrl
    if(context==''){
      return url
    } else {
      return "'${url}/${context}'"
    }
  }

  def curl(action){
    this.createEtherCredentials()

    List cmd = []
    cmd << this.headers
    cmd << action
    cmd << "--cert ${this.getEtherCertificate()}"
    cmd << "--key ${this.getEtherPrivateKey()}"

    def script = "{ set +x; } 2>/dev/null\ncurl --silent --location --insecure ${cmd.flatten().join(' ')}"
    if (this.debug == true) { this.script.echo(script) }

    def result = this.script.sh (script: script, returnStdout: true)
    if (this.debug == true) { this.script.echo result }

    Map resultMap = [:]
    try {
      resultMap = this.script.readJSON text: result
      resultMap.ok = true
    } catch (Exception e) {
      resultMap.ok = false
      resultMap.error = result
    }
    return resultMap
  }

  def post(context, data=null){
    def cmd
    if(data==null){
      cmd = [ '-XPOST', compose(context) ]
    }else{
      def jsonData = JsonOutput.toJson(data)
      cmd = [
        '-XPOST',
        "-d '${jsonData}'",
        compose(context)
      ]
    }
    return this.curl(cmd)
  }

  def get(context=''){
    def cmd = [ '-XGET', compose(context) ]
    return this.curl(cmd)
  }
}

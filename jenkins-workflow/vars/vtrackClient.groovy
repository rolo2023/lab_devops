#!groovy
import api.vtrack.IVtrack
import api.vtrack.v0.Vtrack

private IVtrack init(body, extraConfig = [:]) {
  Map config = [
    baseUrl: 'https://vtrack.central.platform.bbva.com',
    apiVersion: 'v0',
    architecture: 'generic',
    application: 'test',
    debug: false,
    proxy: false
  ]
  config << extraConfig

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  IVtrack vt
  switch(config.apiVersion){
    case 'v0':
        vt = new Vtrack(this, config)
    break
  }
  return vt
}

IVtrack withSecretTextCredentialIds(String etherCertificateCredentialId, String etherKeyCredentialId, body) {
  throw new Exception("withSecretTextCredentialIds not implemented yet")
}

/**
 * Builds a new Vtrack client instance.
 *
 * The Vtrack client instance needs credentials in order to communicate with Vtrack Ether Cloud Service.
 * These credentials will be obtained from the provided Secret File Jenkins credentials.
 *
 * @param etherCertificateCredentialId The Jenkins Secret File credential id holding the Ether user certificate.
 * @param etherKeyCredentialId The Jenkins Secret File credential id holding the Ether user key.
 *
 * @return Vtrack A new Vtrack client instance.
 */
IVtrack withSecretFileCredentialIds(String etherCertificateCredentialId, String etherKeyCredentialId, body) {
    Map extraConfig = [
        credentialFileId: etherCertificateCredentialId,
        keyFileId: etherKeyCredentialId
    ]
    return init(body, extraConfig)
}

/**
 * Guess who has performed the execution of this pipeline
 *
 * @param String The credential Id with read permission in your repository
 *
 * @return String The username of the user who has performed the execution of this pipeline
 */
String getExecutorUsername(String credentialsId){
  String username = ""
  echo "getExecutorUsername"
  if (currentBuild.rawBuild.getCause(jenkins.branch.BranchEventCause) || currentBuild.rawBuild.getCause(jenkins.branch.BranchIndexingCause)){
    if(env.BRANCH_NAME.startsWith("PR-")){
      echo "getExecutorUsername cause: jenkins.branch.BranchEventCause PR"
      username = env.CHANGE_AUTHOR
    } else {
      echo "getExecutorUsername cause: jenkins.branch.BranchEventCause other"
      withCredentials([sshUserPrivateKey(credentialsId: credentialsId, keyFileVariable: 'SSHKEY')]) {
        sh(script: "export GIT_SSH_COMMAND='ssh -i ${SSHKEY} -o StrictHostKeyChecking=no'; git fetch origin refs/notes/push-traceability:refs/notes/push-traceability")
        notes = sh(script: "git notes --ref push-traceability show", returnStatus:true)
        if (notes.equals(0)){
          username = sh(script: "git notes --ref push-traceability show | jq .userName", returnStdout: true)
        }
        else{
          username = sh(script: "git log -n 1 --pretty=format:'%an'", returnStdout: true).trim()
        }
      }
    }
  }else if(currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)){
    echo "getExecutorUsername cause: hudson.model.Cause UserIdCause"
    username = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause).properties.userName
  }
  return username.trim()
}

/**
 * Gets the long commit hash that is being built
 *
 * @return String The long commit hash that is being built
 */
String getCommitHash(){
  return sh(script: "git log -n 1 --pretty=format:'%H'", returnStdout: true).trim()
}

/**
 * Gets the git URL of the repository that is being built
 *
 * @return String The git URL of the repository that is being built
 */
String getGitUrl(){
  return sh(script: 'git config --get remote.origin.url', returnStdout: true).trim()
}

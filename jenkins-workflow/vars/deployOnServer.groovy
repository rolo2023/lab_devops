import globals.*

/**
 * deployment to applications servers
 *
 * Revisar doc/vars/deployOnServer.md
 *
 */
def call(Map map=[:]){

    validate(map)

    def nodeServer = map.node
    def identityApp = map.identityApp ?: Settings.repo.slug

    def environmentToDeploy = map.environmentDeploy ?: 'TEST'
    def remoteGit = map.remoteGit ?: Settings.applicationConfigurationRepo(Settings.country)

    def fileConfApps = "confApps${environmentToDeploy}.yml"
    def fileConfServers = "confServers${environmentToDeploy}.yml"

    Modules.git.getFileFromRepo(fileConfApps, remoteGit, 'master')
    Modules.git.getFileFromRepo(fileConfServers, remoteGit, 'master')

    def apps = readYaml (file: fileConfApps )
    def serversYml = readYaml (file: fileConfServers )

    Helpers.log.debug "deployApplicationServer :: Configuration files downloaded successfully :: ${fileConfApps} | ${fileConfServers}"

    Map confRemoteApp = null;

    try{
      confRemoteApp = apps.applications.find { it.applicationRepo == identityApp }
    }catch(Exception _){
      Helpers.log.warn "deployApplicationServer :: The configuration file has one problem. ¿Exists? Please read the next link https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse/doc/vars/deployOnServer.md"
      return
    }

    if (!confRemoteApp) {
        Helpers.log.warn "deployApplicationServer :: The Application doesn't have the deployment configuration. Please read the next link https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse/doc/vars/deployOnServer.md"
        return
    } 

    //get the servers
    def servers = serversYml.servers

    //validate credentials of servers
    confRemoteApp.components.each { component ->
        servers.each { server -> 
          if(server.hostname == component.hostname){
            validateParamsDeployment(server)
          }
        }
    }

    if (confRemoteApp.authTeam) {
        Helpers.log.debug "deployApplicationServer :: authTeam: ${confRemoteApp.authTeam}"
        if (!Helpers.approval(confRemoteApp.authTeam, map.subjectEmail ?: '', map.messageEmail ?: '', map.waitMinutes ?: 10)){
            Helpers.log.warn "deployApplicationServer :: rejected deployment"
            return
        }
    }

    //if the validation are ok, then the artifacts are downloaded
    def pathFinalFile = downloadArtifacts(map)

    //Installation of components
    confRemoteApp.components.each { component -> 
        def serverTarget = servers.find { server -> server.hostname == component.hostname }
        installComponent(nodeServer, component, serverTarget, pathFinalFile)
    }
}

/**
 * When called automatically from within a step, this will be invoked.
 * Use it to massage parameters before sending them to the 'call' method
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
  this.call(stepArgs)
}

def validate(Map map){

  // Validamos parametros obligatorios
  if (!fromMap(map).checkMandatory([
    'node'
  ])) { return }
}

def validateParamsDeployment(Map map){
  if (!Helpers.credentialExists("rsa", map.ssh_pk_id)) {
      exit "deployApplicationServer :: validate ssh credentials not found in Jenkins"
      return
  }

  if (!Helpers.credentialExists("usernamePassword", map.server_id)) {
      exit "deployApplicationServer :: validate usernamePassword credentials not found in Jenkins"
      return
  }
}

def downloadArtifacts(Map map) {
    def fileDeployable = map.deployablefile ?: ''
    def fileExt = "${fileDeployable}".reverse().take(3).reverse()
    def pathFinalFile = "${fileDeployable}"

    Helpers.getArtifacts(map.deployablefile, map.urlArtifacts)

    if(fileExt == "zip") {
        //extrayendo todos los archivos y moviendolos a la carpeta temporal2 (no importa si el zip contiene subcarpetas)
        def commandUnzip = sh(script: "mkdir -p ./temporal && unzip ${fileDeployable} -d temporal/ && mkdir -p ./temporal2/ &&  find ./temporal/ -iname '*.*' -exec mv '{}' ./temporal2/ \\;", returnStdout: true)
        Helpers.log.debug("deployApplicationServer :: commandUnzip: ${commandUnzip}")

        //solo para ver lo que hay en la carpeta descomprimida
        def commandLsALUnzip = sh(script: "pwd && ls -al temporal2/", returnStdout: true)
        Helpers.log.debug("deployApplicationServer :: ${commandLsALUnzip}")

        pathFinalFile = "temporal2/"
    }

    Helpers.log.debug "deployApplicationServer :: pathFinalFile: ${pathFinalFile}"
    return pathFinalFile
}

def installComponent(def nodeServer, def component, def serverTarget, String pathFinalFile){  

  Helpers.log.debug "deployApplicationServer :: component ${component}"
  Helpers.log.debug "deployApplicationServer :: serverTarget ${serverTarget}"

  if(!pathFinalFile.contains(component.artifact)){
    Helpers.log.debug "deployApplicationServer :: assuming that the path is a folder :: ${pathFinalFile}"
    pathFinalFile += component.artifact
  }

  File file = new File("${pathFinalFile}")
  def fileServer = "./"+file.name
  stash name: "artefacto", includes: "${pathFinalFile}"

  node(nodeServer){
    unstash "artefacto"
    sshagent (credentials: ["${serverTarget.ssh_pk_id}"]) {
      sh "scp -o StrictHostKeyChecking=no ${pathFinalFile} ${serverTarget.ssh_user}@${component.hostname}:./"

      try{
        if(serverTarget.server == "was"){
          deployWAS(component, serverTarget, fileServer)
        } else if (serverTarget.server == "jboss"){
          deployJBoss(component, serverTarget, fileServer)
        } else {
          Helpers.log.error "deployApplicationServer :: Server undefined"
        }
        Helpers.log.info "deployApplicationServer :: Aplicacion ${component.name} instalada correctamente"
      }catch(Exception ex){
        Helpers.log.error "deployApplicationServer :: Catched deployOnServer Module Exception: ${ex}"
      }
    }
    deleteDir()
  }
}

def deployWAS(def component, def server, def fileServer){
  withCredentials([usernamePassword(credentialsId: "${server.server_id}", usernameVariable: 'SERVER_USR', passwordVariable: 'SERVER_PSW')]) {
    def server_host = component.hostname
    server_host = server.server_host == null ? component.hostname : server.server_host

    def deploy_script = libraryResource "deploy/deployWAS.py"
    writeFile file: "deployWAS.py", text: "${deploy_script}"

    sh """
      scp deployWAS.py ${server.ssh_user}@${component.hostname}:./
      ssh ${server.ssh_user}@${component.hostname} '/opt/IBM/WebSphere/AppServer/bin/wsadmin.sh -user ${SERVER_USR} -password ${SERVER_PSW} -host ${server_host} -port 8879 -lang jython -f ./deployWAS.py ${component.clusterName} ${component.name} ${fileServer} ${component.scriptDeploy.bytes.encodeBase64().toString()}'
      ssh ${server.ssh_user}@${component.hostname} 'rm ./deployWAS.py'
    """

    sh "ssh ${server.ssh_user}@${component.hostname} 'rm ${fileServer}'"
  }
}

def deployJBoss(def component, def server, def fileServer){
  withCredentials([usernamePassword(credentialsId: "${server.server_id}", usernameVariable: 'SERVER_USR', passwordVariable: 'SERVER_PSW')]) {
    def server_host = component.hostname
    server_host = server.server_host == null ? component.hostname : server.server_host
    try{
      sh """
        ssh ${server.ssh_user}@${component.hostname} 'jboss-cli.sh -c -u=${SERVER_USR} -p=${SERVER_PSW} --controller=${server_host}:9990 --command="undeploy ${component.contexRoot} --server-groups=${component.serverGroup} "'
      """
      Helpers.log.info "deployApplicationServer :: Aplicación ${component.contexRoot} desinstalada correctamente"
    }catch(Exception ex){
      Helpers.log.info "deployApplicationServer :: Aplicación ${component.contexRoot} no existe para desinstalar"
    }

    sh """
      ssh ${server.ssh_user}@${component.hostname} 'jboss-cli.sh -c -u=${SERVER_USR} -p=${SERVER_PSW} --controller=${server_host}:9990 --command="deploy ${fileServer} --server-groups=${component.serverGroup} --name=${component.contexRoot} --runtime-name=${component.artifact}"'
    """

    sh "ssh ${server.ssh_user}@${component.hostname} 'rm ${fileServer}'"
  }
}

//* Used during testing to return a callable script
return this

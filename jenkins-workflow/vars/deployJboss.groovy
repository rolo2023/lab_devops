import globals.*

/**
 * deployJboss
 *
 * Revisar jenkins-workflow/doc/modules/deployJboss.md
 *
 */
def call(Map map=[:]){

  validate(map)

  def node_server = map.node
  def server_ip = map.server_ip
  def ssh_pk_id = map.ssh_pk_id
  def ssh_user = map.ssh_user
  def server_id = map.server_id
  def warPath = map.warPath
  def server_group = map.server_group
  def contex_root = map.contex_root
  def usersApproval = map.usersApproval

  if(map.usersApproval != null){
    if (!approval(usersApproval)){
      Helpers.log.info "jBoss :: Deploy rechazado"
      return
    }
  }

  File file = new File("${warPath}")
  def fileServer = "./"+file.name
  def fileExt = file.name.split('\\.').last()
  stash name: "artefacto", includes: "${warPath}"
  
  node(node_server){
    unstash "artefacto"
    sshagent (credentials: ["${ssh_pk_id}"]) {
      sh "scp -o StrictHostKeyChecking=no ${warPath} ${ssh_user}@${server_ip}:./"
      withCredentials([usernamePassword(credentialsId: "${server_id}", usernameVariable: 'server_user', passwordVariable: 'server_password')]) {
        def server_host = server_ip
        server_host = map.server_host == null ? server_ip : map.server_host
        try{
            sh """
                ssh ${ssh_user}@${server_ip} 'jboss-cli.sh -c -u=${server_user} -p=${server_password} --controller=${server_host}:9990 --command="undeploy ${contex_root} --server-groups=${server_group} "'
            """
            Helpers.log.info "Aplicación ${contex_root} desinstalada correctamente"
        }catch(Exception ex){
            Helpers.log.info "Aplicación ${contex_root} no existe para desinstalar"
        }
        try{
          sh """
              ssh ${ssh_user}@${server_ip} 'jboss-cli.sh -c -u=${server_user} -p=${server_password} --controller=${server_host}:9990 --command="deploy ${fileServer} --server-groups=${server_group} --name=${contex_root} --runtime-name=${contex_root}.${fileExt}"'
          """
          Helpers.log.info "Aplicación ${contex_root} instalada correctamente"
        }catch(Exception ex){
            Helpers.log.error "Catched deployJboss Module Exception: ${ex}"
        }
      }
      sh "ssh ${ssh_user}@${server_ip} 'rm ${fileServer}'"
    }
    deleteDir()
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
    'node', 'server_ip', 'ssh_pk_id', 'ssh_user', 'server_id', 'warPath', 'server_group', 'contex_root'
  ])) { return }

  // Validamos existencia de llave privada en jenkins
  if (!Helpers.credentialExists("rsa", map.ssh_pk_id)) {
      exit "jBoss :: ssh credentials not found in Jenkins"
      return
  }

  // Validamos existencia de credenciales en jenkins
  if (!Helpers.credentialExists("usernamePassword", map.server_id)) {
      exit "jBoss :: usernamePassword credentials not found in Jenkins"
      return
  }
}

def approval(String users) {
    String[] users_ = "${users}".split(',')
    String mails = ""
    for (String s: users_) {
        mails = mails + s + "@bbva.com,"
    }

    def userInput
    def subject

    subject = "Jenkins - Despliegue en jBoss"
    emailext (
        subject: "${subject}",
        body: """
        <h3>${subject}</h3>
        <p>Un despliegue requiere su aprobación</p>
        <p>Para aprobar el despliegue hacer clic&nbsp; <a href="${env.BUILD_URL}/input/"> <span style="background-color: #003366; color: #ffffff; display: inline-block; padding: 3px 10px; font-weight: bold; border-radius: 5px;"> <span style="text-decoration: underline;">Aqu&iacute;</span></span></a>.</p>
        """,
        to: "${mails}"
    )
    try {
        echo "Esperando aprobación de ${users}"
        timeout(time: 10, unit: 'MINUTES') {
            userInput = input(
                submitterParameter: 'approval',
                submitter: "${users}",
                message: "¿Desplegar en Servidor jBoss?", parameters: [
                [$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'Aprobar']
            ])
        }
    } catch(err) {
        Helpers.log.error "jBoss :: Se supero el tiempo de espero del usuario ${users}"
    }
    if(userInput != null){
        if (userInput.Aprobar == true) {
            Helpers.log.info "jBoss :: Aprobado por ${userInput.approval}"
            return true
        }else{
            return false
        }
    }else{
        return false
    }
}

//* Used during testing to return a callable script
return this

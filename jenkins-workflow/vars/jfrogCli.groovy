import globals.Helpers

/**
 * jfrogCli
 *
 * TODO: description
 *
 * Mandatory params:
 * -
 * -
 */
def call(Map config){

  fromMap(config).checkMandatory(['credentialsId', 'repo', 'pathRepo', 'dirPath'])

  def credentials = getGredentials(config.credentialsId)
  publish(config, credentials)

}

/**
 * When called from within a step, this will be invoked AUTOMATICALLY.
 * Use it to massage parameters before sending them to the 'call' method
 *
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map args) {

  def server = args.server ?: 'https://globaldevtools.bbva.com/artifactory/'
  def dirRepo = args.dirRepo.replace("%2F", "_").replace("/", "_") + '/' ?: ''
  def fileName = (args.fileName) ?: ''

  Map cfg = [
    server: server,
    credentialsId: args.credentialsId,
    repo: args.repo + '/',
    pathRepo: args.pathRepo + '/',
    dirRepo: dirRepo,
    fileName: fileName,
    dirPath: args.dirPath
  ]

  call cfg
}

def getGredentials(credentialsId){
  withCredentials([usernamePassword(credentialsId: credentialsId, passwordVariable: 'PASS', usernameVariable: 'USER')]) {
    return [
      user: USER,
      pass: PASS
    ]
  }
}

def publish(config, credentials) {
  def fullpath = config.repo + config.pathRepo + config.dirRepo
    sh """
      cd ${config.dirPath}
      jfrog rt u --url ${config.server} --user ${credentials.user} --password ${credentials.pass} \
                 --build-name ${Helpers.jenkins.env.JOB_BASE_NAME} \
                 --build-number ${Helpers.jenkins.env.BUILD_NUMBER} \
                 --regexp=true --flat=false ./${config.fileName} ${fullpath}
    """
}

return this

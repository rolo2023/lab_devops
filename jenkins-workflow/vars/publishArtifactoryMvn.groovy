import globals.Helpers

/**
 * publishArtifactory
 *
 * TODO: description
 *
 * Mandatory params:
 * -
 * -
 */
def call(Map map){

  validate(map)

  checkMavenHome()

  def path_pom = map.path_pom ?: "./pom.xml"
  //def artifactory_repo = map.artifactory_repo
  def artifactory_id = map.artifactory_id
  def command = map.command

  def releases
  def snapshots
  def repository 

  if (map.artifactory_repo) {
  
	def artifactory_repo = map.artifactory_repo
	
	releases = artifactory_repo + "-releases"
	snapshots = artifactory_repo + "-snapshots"
	repository = "repository-" + artifactory_repo
	
  } else if (map.custom_repo) {
  
	// Validamos parametros obligatorios
	if (!fromMap(map.custom_repo).checkMandatory([
	  'release', 'snapshot', 'repository'
	])) { return }
  
	releases = map.custom_repo.release
	snapshots = map.custom_repo.snapshot
	repository = map.custom_repo.repository
  
  } else {
	
	exit "Missing mandatory: artifactory_repo OR custom_repo"
    return 
	
  }

  String artifactory_url = 'https://globaldevtools.bbva.com/artifactory-api/'

  def server = Artifactory.newServer url: artifactory_url, credentialsId: "${artifactory_id}"
  def rtMaven = Artifactory.newMavenBuild()
  rtMaven.resolver server: server, releaseRepo: repository, snapshotRepo: repository
  rtMaven.deployer server: server, releaseRepo: releases, snapshotRepo: snapshots
  def buildInfo = rtMaven.run pom: path_pom, goals: command
  buildInfo.env.capture = true
  buildInfo.env.collect()
  server.publishBuildInfo buildInfo

}

def validate(Map map){

  if (!Helpers.pluginExists("artifactory")){
    Helpers.log.error 'Please Install the plug-in artifactory. Visit: https://www.jfrog.com/confluence/display/RTF/Jenkins+Artifactory+Plug-in'
    exit "publishArtifactory :: Plugin Artifactory not found in Jenkins"
    return
  }

  // Validamos parametros obligatorios
  if (!fromMap(map).checkMandatory([
    'artifactory_id', 'command'
  ])) { return }
  

  // Validamos existencia de credenciales en jenkins
  if (!Helpers.credentialExists("usernamePassword", map.artifactory_id)) {
      exit "publishArtifactory :: usernamePassword credentials not found in Jenkins"
      return
  }
}

def checkMavenHome() {

  if (env.MAVEN_HOME == null) {
  
	def maven_home = sh (returnStdout: true, script: """
		echo \$MAVEN_HOME
	""").trim()
	
	Helpers.log.info "MAVEN_HOME '${maven_home}' leído del entorno actual"
	
	env.MAVEN_HOME = maven_home
	
  }

}

/**
 * When called automatically from within a step, this will be invoked.
 * Use it to massage parameters before sending them to the 'call' method
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
  call(stepArgs)
}

//* Used during testing to return a callable script
return this

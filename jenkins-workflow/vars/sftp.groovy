import globals.Helpers

/*sftp wrapper to be used in steps
  - localDir:       Local localization of artifacts
  - remoteDir:      Remote localization where artifacts will be deployment
  - remoteUser:     User server
  - remoteIp:       Ip destination server
  - remoteKey:      ID of the SSH key for connection to the destination server
  - deployableFile: Name for unstash
  - remoteCommand:  Command line that will be executed
  - userCredential: ID credential in Jenkins
  - groupGraaS:     Name of the gras group that have permissions to deploy 
*/
def call(Map configuration) {
    if (!fromMap(configuration).checkMandatory([
        'localDir', 'remoteDir', 'remoteUser', 'remoteIp', 'remoteKey', 'deployableFile', 'remoteCommand', 'userCredential' ,'groupGraaS'
    ])) { return }

    localDir            = configuration.localDir
    remoteDir           = configuration.remoteDir
    remoteUser          = configuration.remoteUser
    remoteIp            = configuration.remoteIp
    remoteKey           = configuration.remoteKey
    deployableFile      = configuration.deployableFile
    remoteCommand       = configuration.remoteCommand
    userCredential      = configuration.userCredential
    groupGraaS          = configuration.groupGraaS

    // Check that credentials exist, and that they are of the expected type
    Boolean invalidCredentials = [
            "userpass: ${configuration.userCredential}",
    ].collect { it -> Helpers.credentialExists(it) }.any { !it }
    if (invalidCredentials) {
        exit "libRunTests :: Invalid credentials are sent"
        return
    }

    //Check that user have permissions for deploy  
    if ( Helpers.isCurrentUserInGraasGroup(groupGraaS) ){//Compare group GRaaS with group of user 
        Helpers.log.info "CheckPermissions :: You have access for deploy"
    }
    else{
        exit "CheckPermissions :: You don´t have access for deploy"
    }

    sshagent (credentials: ["${remoteKey}"]) {
        withCredentials([Helpers.getCredential('usernamePassword', userCredential, 'USER_WASC')]) {
            unstash "${deployableFile}"
            //upload packages
            uploadPackages(remoteUser, remoteIp, localDir, remoteDir)
            //Uncompress
            execRemoteCommand(remoteUser, remoteIp, remoteCommand)
        }
    }
}

def fromStep(Map step){ return call(step) }

private execRemoteCommand(pRemoteUser, pRemoteIP, pRemoteCommand){
    try{    
        sh "ssh ${pRemoteUser}@${pRemoteIP} \"${pRemoteCommand}\""
        return true
    } catch (Exception ex) {
        exit "CommandRemote :: Fail executing remote command ${pRemoteCommand} with user ${pRemoteUser} in the ip${pRemoteIP} "
        return false
    }
}

private Boolean uploadPackages(pRemoteUser, pRemoteIP, pLocalDir, pRemoteDir){
    try{
    sh "scp -v -o StrictHostKeyChecking=no ${pLocalDir}${deployableFile} ${pRemoteUser}@${pRemoteIP}:${pRemoteDir}"
    return true
    } catch (Exception ex) {
        exit "UploadPackages :: Fail importing files in ${pRemoteDir} from ${pLocalDir}${deployableFile} with user ${pRemoteUser} in the ip${pRemoteIP}"
        return false
    }
}

return this
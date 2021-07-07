import globals.Helpers
import globals.Settings

/**
 * fromLibrary
 *
 * TODO: description
 *
 * Mandatory params:
 * -
 * -
 */
def call(Map map){

  if (!validate(map)) {
    error "fromLibrary :: input parameter validation failed for ${map}"
    return
  }

  if(map.containsKey("library")) {
    loadLibrary(map.library)
  } else {
    Helpers.log.warn "No Library loading"
  }

  execCmd(map.command)
}

/**
 * Checks if all the conditions needed in order to execute fromLibrary
 *  are valid.
 * This should include input parameter validation, user/group permissions,
 *  mandatory plugins are present, etc...
 *
 * @param config Map with all the entries sent to the module in the
 *              'with_params' section of the configuration
 */
Boolean validate(Map config) {

  if (!fromMap(config).checkMandatory(['command'])) { 
    return
  }

  if(config.containsKey("library")) {
    if (!fromMap(config.library).checkMandatory(['identify', 'remote', 'credentialsId'])) { 
      return
    }
  }
  
  return true
}

def loadLibrary(Map args) {
  library identifier: args.identify, retriever: modernSCM([
      $class: "GitSCMSource", 
      remote: args.remote, 
      credentialsId: args.credentialsId
  ])
}

def execCmd(String cmd) {
  Eval.xy(Helpers.jenkins, Settings, "x."+cmd)
}

/**
 * When called from within a step, this will be invoked AUTOMATICALLY.
 * Use it to massage parameters before sending them to the 'call' method
 *
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
  call(stepArgs)
}

//* Used during testing to return a callable script
return this

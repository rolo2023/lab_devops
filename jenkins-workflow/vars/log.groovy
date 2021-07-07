import globals.Helpers

def call(Map args) {
    this.fromStep(args)
}

def warn(message) { fromStep([level: 'warning', message: message]) }

def error(message) { fromStep([level: 'error', message: message]) }

def info(message) { fromStep([level: 'info', message: message]) }

def debug(message) { fromStep([level: 'debug', message: message]) }

def fromStep(Map stepArgs) {
    String message = stepArgs.get('message', null)
    if (message != null) {
        String logLevel = stepArgs.get('level', 'INFO').toUpperCase()
        message = Helpers.subst(message)
        Helpers.log.say(logLevel, message)
    }
}

return this
import globals.*

def call(Map settings) {
    if ('pod_def' in settings.keySet()) {
        return readFile(file: Settings.getRemoteConfig(settings.pod_def))
    } else {
        return libraryResource('pod_def.yml')
    }
}

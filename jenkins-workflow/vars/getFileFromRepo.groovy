/**
 * Downloads a single file, for a given revision of a GIT repository.
 * Assumes current GIT credentials are enough to access it.
 *
 * Git version must be > 1.8, so we run this inside the 'generic' node
 *
 * @param file      - name of the object to extract from the repo
 * @param remote    - Remote repository to read from
 * @param revision  - Branch/tag we want to use (defaults to HEAD)
 *
 */
def call(Map args){
    fromMap(args).checkMandatory(['file', 'remote'])

    def remote = args.remote
    def file = args.file
    def revision = args.revision ?: 'HEAD'
    def stashName = "stash_${file}"

    try {
        globals.Modules.git.run "git archive --remote=${remote} ${revision} ${file} | tar -xvf -"
    } catch (gitArchiveException) {
        globals.Helpers.error "Helpers :: getFileFromRepo(${file}, ${remote}) :: Cannot get remote file :: ${gitArchiveException}"
    } catch (anything) {
        globals.Helpers.error "Helpers :: getFileFromRepo(${file}, ${remote}) :: Unexpected error: ${anything}"
    }
}

def fromStep(Map step){ return call(step) }

return this

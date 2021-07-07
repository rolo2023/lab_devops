def call(String message){
    log.error message
    error 'Workflow halted. See errors above.'
}

return this

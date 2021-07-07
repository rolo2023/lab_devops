def call(Map args){
    fromMap(args).checkMandatory(['action', 'filename'])
    try {
         this."${action}"(args.filename, args.target, args.utility)
    } catch (MissingMethodException ex) {
        error "Unknown or unavailable action '${args.action}'"
    }

}

void extract(String filename, String target='.', String utility='unzip'){
    if(!target) target = '.'
    if(!utility) utility = 'unzip'
    sh "${utility} ${filename} -d ${target}"
}

void compress(String path, String target=null, String utility='zip'){
    if(!target) target = "${path}.zip"
    if(!utility) utility = 'zip'
    sh "${utility} ${target} ${path}"
}
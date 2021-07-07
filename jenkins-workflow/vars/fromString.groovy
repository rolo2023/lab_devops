String str

def call(String str){
    this.str = str
    return this
}

String jinja(source, defaults=''){
    string = this.str

    def jinjaRegex = /\{\{\s?([\w\.?]+)\s?\}\}/
    (string.findAll(jinjaRegex){ it }).each { key ->
        string = string.replace(key[0], fromMap(source).deepGet(key[1], defaults))
    }

    return string
}

return this

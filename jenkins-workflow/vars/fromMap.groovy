Map map

def call(Map m){
    this.map = m ?: [:]
    return this
}

Boolean checkMandatory(String needed){ return this.checkMandatory([needed]) }

Boolean checkMandatory(List needed){
    List missing = needed.findAll ({ !(it in this.map.keySet()) })
    if (missing) {
        exit "Missing mandatory: ${missing.join(', ')}"
        return false
    }

    List anyEmpty = needed.findAll { ! this.map[it] }
    if (anyEmpty) {
        exit "Empty mandatory values: ${anyEmpty.join(', ')}"
        return false
    }
    return true
}

List toEnv(){
    List environList = []
    this.map.each { k, v -> environList << "${k}=${v}" }
    return environList
}

def deepGet(String keyWithDots, String defaults=''){
    Map m = this.map
    List dots = keyWithDots.split('\\.')
    if(dots.size() < 2) return defaults // dotted key size MUST be at least 2 elements size

    dots[0..-2].each {
        m = m.get(it, [:])
    }
    return m.get(dots.last(), defaults)
}


return this

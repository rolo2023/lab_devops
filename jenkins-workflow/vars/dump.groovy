import static groovy.json.JsonOutput.*

def call(item){
    switch(item){
        case [ Closure ]:
            return item.toString()
        default:
            return prettyPrint(toJson(item))
    }
}

return this

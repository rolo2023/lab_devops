#!groovy
package api.requests

class Session implements Serializable {

    private Map headers = [:]
    private String baseUrl = null
    private boolean proxyEnabled = false
    transient Proxy proxyData // Proxy class is not serializable
    private protectedHeaders = ['Authorization']

    Session(args) {
        // First of all, baseUrl cannot be blank
        if(!args.baseUrl){ throw new Exception('EmptyBaseURL') }

        // Set headers (JSON by default)
        if(args.headers){ headers << args.headers }

        // Uses Proxy?
        if (args.proxy) {
            def proxyUrl = args.proxy
            if(!proxyUrl.startsWith('http')){ proxyUrl = "http://${proxyUrl}" }
            URL proxy = new URL(proxyUrl)
            this.proxyEnabled = true
            this.proxyData = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()))
        }

        // And finally
        this.baseUrl = args.baseUrl
    }

    void setUrl(String url){ this.baseUrl = url }
    String getUrl(){ return this.baseUrl }

    void setHeader(key, value){ this.headers.put(key, value) }
    void removeHeader(String key){ if(this.headers[key]) {this.headers.remove(key) } }

    // Don't return a header marked as protected
    String getHeader(String key){
        if(!this.protectedHeaders.contains(key)){
            return this.headers.get(key)
        }
    }

    void enableProxy(boolean enable){ this.proxyEnabled = enable }
    void enableDebug(boolean state){ this.debug = state }

    Map get(String requestUrl='', Map overrides=[:]){ 
        return request('GET', requestUrl, null, overrides)
    }

    Map post(String requestUrl='', Map jsonMap=[:], Map overrides=[:]){
        return request('POST', requestUrl, jsonMap, overrides)
    }

    Map put(String requestUrl='', Map jsonMap=[:], Map overrides=[:]){
        return request('PUT', requestUrl, jsonMap, overrides)
    }

    Map delete(String requestUrl='', Map overrides=[:]){
        return request('DELETE', requestUrl, overrides)
    }

    private Map request(method='GET', context='', Map jsonMap=[:], Map overrides){
        def requestUrl = this.baseUrl
        def jsonSlurper = new groovy.json.JsonSlurper()
        def json

        // Get connection object, given the headers and configs
        HttpURLConnection connection
        if(context != ''){ requestUrl = "${requestUrl}/${context}" }
        URL url = new URL(requestUrl)

        // Proxy
        if(this.proxyEnabled){
            connection = url.openConnection(this.proxyData)
        } else {
            connection = url.openConnection()
        }
        
        // Headers
        def headers = this.headers
        if(overrides.headers){ headers << overrides.headers }
        headers.each() { header, content ->
            connection.setRequestProperty(header, content)
        }
        connection.setRequestMethod(method)

        // If this is a PUT or POST, convert the JSON into a stream
        if(method == 'PUT' || method == 'POST' ){
            //write the payload to the body of the request
            connection.doOutput = true
            json = groovy.json.JsonOutput.toJson(jsonMap)
            def writer = new OutputStreamWriter(connection.outputStream)
            writer.write(json)
            writer.flush() 
            writer.close()
        }

        // Connect and get response
        connection.connect()
        def statusCode = connection.responseCode
        def message = connection.responseMessage

        def result = [ ok: false, error: false ]
        if(statusCode == 200 || statusCode == 201){
            if(connection.getContentLengthLong() > 0){
                try {
                    result << jsonSlurper.parseText(connection.content.text)
                } catch (Exception isNotJson){
                    try {
                        result.message = connection.content.text 
                    } catch (Exception emptyMessage){
                        result.message = message
                    }
                }
            }
            result.ok = true
        } else {
            def errorMsg = connection.getErrorStream()
            if(errorMsg){ errorMsg = errorMsg.text.trim() }
            result.error = [
                message: strToMap(errorMsg),
                url: requestUrl,
                headers: this.headers,
                method: method,
                request: json
            ]
        }
        connection.disconnect()
        
        return result
    }

    private Map strToMap(String jsonStr){
        def jsonSlurper = new groovy.json.JsonSlurper()
        def newMap = [:]
        try {
            newMap = jsonSlurper.parseText(jsonStr)
        } catch (Exception ex){
            newMap = [ text: jsonStr ]
        }
        return newMap
    }

}

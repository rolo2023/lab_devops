#!groovy
package api.requests
import org.apache.commons.codec.binary.Base64

class JsonClient implements Serializable {

    private Map headers = [:]
    private String baseUrl = null
    private def script
    private boolean proxyEnabled = false
    transient Proxy proxyData // Proxy class is not serializable
    private protectedHeaders = ['Authorization']

    JsonClient(script, args) {
        // First of all, baseUrl cannot be blank
        if(!args.baseUrl){ throw new Exception('EmptyBaseURL') }

        // Set headers
        def headers = [:]
        headers['Content-Type'] = 'application/json'
        headers['Accept'] = 'application/json,text/plain'
        if(args.headers){ headers << args.headers }

        if (args.auth){
            def method = args.auth[0]
            def credentials = args.auth[1]
            switch(method){
                case 'basic':
                    String encodedBytes = Base64.encodeBase64String(credentials.getBytes())
                    headers['Authorization'] = "Basic ${encodedBytes}"
                    break
            }
        }
        this.headers = headers

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
        this.script = script
    }

    def setUrl(String url){ this.baseUrl = url }
    def getUrl(){ return this.baseUrl }

    def setHeader(key, value){ this.headers.put(key, value) }
    def removeHeader(String key){ if(this.headers[key]) {this.headers.remove(key) } }

    // Don't return a header marked as protected
    def getHeader(String key){
        if(!this.protectedHeaders.contains(key)){
            return this.headers.get(key)
        }
    }

    def enableProxy(boolean enable){ this.proxyEnabled = enable }
    def enableDebug(boolean state){ this.debug = state }

    def get(String requestUrl='', Map overrides=[:]){ return request('GET', requestUrl, null, overrides) }
    def post(String requestUrl='', Map jsonMap=[:], Map overrides=[:]){ return request('POST', requestUrl, jsonMap, overrides) }
    def put(String requestUrl='', Map jsonMap=[:], Map overrides=[:]){ return request('PUT', requestUrl, jsonMap, overrides) }
    def delete(String requestUrl='', Map overrides=[:]){ return request('DELETE', requestUrl, overrides) }

    def request(method='GET', context='', Map jsonMap=[:], Map overrides){
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
                message: errorMsg,
                url: requestUrl,
                headers: this.headers.findAll{ key, value -> key != 'Authorization' },
                method: method,
                request: json
            ]
        }
        connection.disconnect()
        
        return result
    }
}

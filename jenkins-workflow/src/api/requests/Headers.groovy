package api.requests
import org.apache.commons.codec.binary.Base64
 
class Headers {

    static String encode(String text){
        return Base64.encodeBase64String(text.getBytes())
    }

    final static Map JSON = [
        'Content-Type' : 'application/json',
        'Accept' : 'application/json,text/plain'
    ]
    
}
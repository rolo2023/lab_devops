# Curl

CURL wrapper

## Mandatory parameters:
 - url
 - action

### Mandatory params for PUT
 - localFile: path to local file where remote will be copied to

## Optional parameters:
 - credentials: 'username:password' used to identify curl via HTTP headers
 - proxy: 'proxyURL' is used to select a proxy for the HTTP connectio
 - headers: headers in list form (eg: [ 'Accept: text/json' ])


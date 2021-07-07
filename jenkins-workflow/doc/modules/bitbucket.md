# bitbucket module (mandatory)
This module configures user credentials for _Bitbucket repository_ and _Bitbucket API calls_. 

> Each feature requires its own credential ID; but it's possible to use the same

The configuration of this module is **mandatory**, as it takes care of the processes referred to continuous integration from code.

## Parameters

- **url**: Bitbucket url.
- **credentialsId** : Credentials ID of type **_ssh key_** in Jenkins that will be used for git commands (clone, fetch, push, etc).
- **credential_type (default: userpass)**: Type of credentials stored. Accepts the following:
    - _userpass_: equivalent to _username with password_. This is the default value.
    - _string_: equivalent to _secret text_, in base64.
    

## Example
```yml
  bitbucket:
    url: https://globaldevtools.bbva.com/bitbucket
    credentialsId: 'spring_{{country}}_{{group}}_git_token'
    credential_type: userpass
```


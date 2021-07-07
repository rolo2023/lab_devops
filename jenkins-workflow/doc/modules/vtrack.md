# Vtrack module

Vtrack is an [Ether](https://platform.bbva.com/en-us/developers/intro/documentation/what-is-ether) service that tracks all that gets built in Jenkins: builds, artifacts and deployments.

## Setup

Documenting how to start using an Ether service is out of scope of this documentation, check the docs at [platform](https://platform.bbva.com/en-us/developers/introduction-ecs/documentation/overview), or the [official guide](https://docs.google.com/document/d/18fJK-UPOrEyqCcMLCr0_48owith8OZRn4d6p9Y3PVP8/edit#)

In a nutshell, you will need:

* A pair of Ether credentials, valid for the _central_  (or _lab-01_) regions 
* A namespace, or someone who can give access to the application namespace to those credentials


## Support

This library has been developed by the Devops Adoption Team. Tickets that relate to how this library works, errors when communicating with Ether API, etc... should be reported to them

The Vtrack Ether library has been developed by the Devops Cross Team. Tickets that relate to new features or bugs in the Ether service should be reported to them.


## Namespaces

You already should understand [what a namespace is](https://platform.bbva.com/en-us/developers/introduction-ecs/documentation/namespaces), 
so let's just see how to use one with this module.

> Either one of these methods has to work, Vtrack module cannot work without a namespace.  

### UUAA application Namespace

Usually, UUAAs will have their namespaces managed by 'Government'.
Rather than hardcoding the namespace in the configuration, we will get the value from the Government API.
 
For this, we'll use `country` plus `uuaa` (either global setting, or local Vtrack setting)

### Non-UUAA

In this case, we'll use the `namespace` setting in the Vtrack configuration

## Default configuration

These are the values that will be used by default, to be overriden by your application config.

```yml
  vtrack:
    # If unset, Vtrack will not be used. This is NOT ideal
    enabled: yes

    # If set, Vtrack exceptions will not break your build
    should_ignore_error: no

    # You should not change this, unless you know what you are doing
    base_url: 'https://vtrack.central.platform.bbva.com'
    
    # If set, we'll point to Vtrack in the lab-01 region
    debug: no
    
    # These will be retrieved via platform or https://pkiglobal-ra.live.es.nextgen.igrupobbva/
    # Make sure they work in the lab-01 region when debugging!
    ether_certificate: 'vtrack_ether_certificate'
    ether_key: 'vtrack_ether_key'

    # If set here, it will be used, along with your country, to retrieve namespace
    # NOTE: Overrides global UUAA setting, to be deprecated
    uuaa: ''

    # If you do not have an UUAA we will write in this namespace -assuming your credentials have write access
    # DO NOT SET THIS VALUE if you have an UUAA
    namespace:  ''
    
    # One of 'GLO', 'ESP', 'MEX', 'PER', 'COL', 'USA', 'ARG', 'CHL', 'PRY', 'PRT', 'TUR', 'URY', 'VEN'
    country: 'ESP'

    # Usually 'spring.r1', 'nacar.r1' or 'generic', check Vtrack API for the full list
    architecture: 'generic'
```

## Sample configuration

Most of the time, just the credentials are going to be used.

```yml
  vtrack:
    namespace:  'enax_console_prod'
    ether_certificate: 'vtrack_enax_cert'
    ether_key: 'vtrack_enax_key'
```

## More Info

* [Codelabs on Vtrack Shared Library usage](https://platform.bbva.com/codelabs/devops-clan/DevOps%20Codelabs#/devops-clan/vTrack%20Shared%20Library%20Tutorial/Introduction/) - while we leverage that library to communicate with the service, it has a nice part on how to set things up.
 
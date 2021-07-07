# Sonar wrapper

Wrapper over the Sonar library, in the LTS version, so it can be run anywhere

## Known Issues

If your Sonar stage complains with an error like this:

```
10:50:03  + git log '--pretty=format:{%n "commit": "%H",%n "parent": "%P",%n  "author": {%n    "name": "%aN",%n    "email": "%aE",%n    "date": "%ad"%n  }%n},' --date=format-local:%Y%m%dT%H%M%SZ
10:50:03  + sed 's/\\/\\\\/g'
10:50:03  fatal: unknown date format format-local:%Y%m%dT%H%M%SZ
10:50:03  [Pipeline] }
```

Then your GIT version is not high enough for the latest Sonar version. We recommend setting the version manually in the
module config:

```
      - use: 'wSonar'
        with_params:
          qualityProfile: 'my-qg-name'
          waitForQualityGate: true
          versionLibSonar: 1.5.7
```

## Parameters:

- useCredentialsArtifactory: enable credentialsId from Artifactory Module if you use 'mvn' and not use 'sonar-scanner',
  default: false
- versionLibSonar: can overwrite the default version LibSonar (default version is '*lts*')
- command: can overwrite the default command 'sonar-scanner' and use, for example, 'mvn sonar:sonar'
- qualityProfile
- qualityGate
- waitForQualityGate: default is false
- enableIssuesReport: default is true
- enableQgReport: default is true
- parameters

## Vtrack information

Upon finish, current Vtrack Build's metadata is updated with the result of the analysis (success or failure).

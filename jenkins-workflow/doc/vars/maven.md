
# Maven

 Maven wrapper to be used in steps

## Parameters

### Mandatory
 - mavenSettings: either a 'file: name' or a 'env: PREFIX' entry
 - goal

### Optionals
 - with_cachelo: Use cachelo to leverage mvn cache
  - key: S3 Key used to upload/download cache
  - paths: List of paths to upload to S3


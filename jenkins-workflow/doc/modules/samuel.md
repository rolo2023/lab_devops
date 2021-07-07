# Samuel (optional)

Wrapper for executing Samuel stage-related behaviors for the project 

## Installing plugin

In order to use this module, you need to install the [official plugin](https://dev.globaldevtools.bbva.com/samuel/web/docs/tasks/samuel-plugins/jenkins-plugin.html#installing-samuel-plugin-for-jenkins).

> Important: Any version above 1.7.10 works but we recommended the version 1.8.3.1.

## Parameters

There are certain parameters **in your application configuration file** that are needed for this to work.

> Important: they are used in order to create an entity. Once it is created, they won't get used anymore

| Name | Mandatory? | Description | Default value |
|------|:----------:|:------------|----------------|
| enabled |  No | either `true` or `false` |`true` |
| owners | No | List of emails, each one of them will have edition rights on the Samuel Entity | None |
| country | No | Country used when creating/searching an Entity | Country specified in the Jenkinsfile



You can get more information about these [here](https://globaldevtools.bbva.com/samuel/web/docs/concepts/behaviors/entities.html)


## Sample configuration

```yaml
modules:
  samuel:
    owners:
      - "juan.arias.freire@bbva.com"
```

## Sample configuration with country

```yaml
modules:
  samuel:
    owners:
      - "juan.arias.freire@bbva.com"
    country: "es"
```
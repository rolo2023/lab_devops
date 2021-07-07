# Defining custom Agent tags

> This section only applies to Jenkins Enterprise Kubernetes

On 09-12-2020 we hotfixed v4.2 so we could add a custom agent tag for both Deploy and Build Pod Templates.

## What is an agent tag?

In Kubernetes, one can define a label (tag) for any pod definition you use:

```
agent {
    kubernetes {
        label 'Agent label'
        yaml 'maven-3-jdk-8-template.yml'
    }
}
```

At any point a job needs to use a pod template, it will first check if one with the same tag already exists, and use it if it does.

## What does this mean? Will it reuse resources?

Yes it will.

If the Pod Template is currently loaded by **any running job in your master**, it will be used, instead of re-defining it.

This means that all the containers your job uses (in the scope of that `agent` block) will come from that Pod Template.

Which may become a problem.

## My job is failing, it seems to be loading a random container I have not defined.

That is the problem: 

* if the label is too generic (`'lolailo'`), every job will reuse the same template, and it will most probably not have the required containers.
* if the label is too specific (e.g. a random one) no resource sharing will ever be done, reducing the amount of concurrent jobs your master can have

So we have to find a middle ground.

# Defining reusable agent tags

> This section is only relevant to users of STD Pipeline

## Default case: no agentTag definition

If no ``agentTag`` parameters is found, we will create a **hash** of the [Settings object](../../src/globals/Settings.groovy), but only after we have loaded the following:

* A remote configuration object
* Local (Jenkinsfile) configuration values

This ensures that resource sharing **only happens** when jobs share the following:

1. A [configuration file](../setting-up-group-file.md)
2. Specific values for the templates/containers defined within, that can be modified by local values (for example, a variable in the ``vars`` section of the Jenkinsfile).

## 'agentTag' defined in Jenkinsfile

If a value for the ``agentTag`` parameter is found in the main Jenkinsfile, tags for build and deploy Pod Templates will be defined as

* ``build-agent-${agentTag}``
* ``deploy-agent-${agentTag}``

This way, Devops Engineers can choose which jobs will share resources, based on their local needs.
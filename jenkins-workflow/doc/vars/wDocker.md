# Docker wrapper

Abstraction of the underlying Docker engine, so that we can build, run or publish to a Registry from within a Pipeline step

## Jenkins Kubernetes

When using this module in a Jenkins Kubernetes master, it will relay the call to the [Kaniko module](kaniko.md)

### Parameters:
- action: One of 'push', 'run' or 'build'
- name
- version (optional)

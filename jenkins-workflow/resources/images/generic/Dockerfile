FROM alpine:3.10

RUN set -ex \
  && apk update \
  && apk add --no-cache \
      curl \
      openssh \
      git

# Kubernetes in Jenkins needs the user and group ID to be 100
RUN set -ex \
  && addgroup -g 1000 -S jenkins \
  && adduser -h /home/jenkins  -D -u 1000 -G jenkins jenkins \
  && chown -R jenkins: /home/jenkins

WORKDIR /home/jenkins
USER jenkins

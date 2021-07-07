## GLibC needed for docker-compose (also useful for oracle-jdk)
## This image is almost as little as the official alpine
FROM frolvlad/alpine-glibc

ARG user=jenkins
ARG group=jenkins
ARG uid=1001
ARG gid=1001
ARG version_dockercompose=1.24.0

RUN apk update \
    && apk add --no-cache openjdk8 git openssh curl bash \
    && addgroup -g ${gid} ${group} \
    && adduser -D -h /home/${user} -u ${uid} -G ${group} -s /bin/bash ${user}

WORKDIR /home/${user}
USER ${user}

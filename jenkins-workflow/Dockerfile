FROM alpine:3.8

ARG user=jenkins
ARG group=jenkins
ARG uid=10000
ARG gid=10000

RUN apk update

RUN apk add \
    openjdk8 bash git openssh curl wget

RUN rm -rf /var/cache/apk/*

RUN addgroup -g ${gid} ${group}
RUN adduser -D -h /home/${user} -u ${uid} -G ${group} -s /bin/bash ${user}

WORKDIR /home/${user}
USER ${user}

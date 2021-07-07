FROM globaldevtools.bbva.com:5000/piaas/alpine-python2.6

ARG user=jenkins
ARG group=jenkins
ARG uid=10000
ARG gid=10000

COPY files/dmcli /usr/local/bin/dmcli
COPY files/dimensions.json /us/xpjenk1/.config/dimensions.json

RUN apk update \
    && apk add \
        openjdk8 bash git openssh curl wget \
    && rm -rf /var/cache/apk/* \
    \
    && addgroup -g ${gid} ${group} \
    && adduser -D -h /home/${user} -u ${uid} -G ${group} -s /bin/bash ${user} \
    \
    && chmod +x /usr/local/bin/dmcli


WORKDIR /home/${user}
USER ${user}

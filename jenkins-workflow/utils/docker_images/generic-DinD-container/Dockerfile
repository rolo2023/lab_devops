FROM globaldevtools.bbva.com:5000/piaas/generic:glibc

ARG version_dockercompose=1.24.0
ARG gid=992

USER root
RUN addgroup -g ${gid} -S docker && addgroup jenkins docker

RUN curl -L "https://github.com/docker/compose/releases/download/${version_dockercompose}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
    && chmod +x /usr/local/bin/docker-compose

USER jenkins

VOLUME /tmp
VOLUME /var/run/docker.sock
WORKDIR /tmp

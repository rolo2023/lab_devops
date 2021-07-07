FROM maven:3.5.3-jdk-8

USER root
RUN set -ex \
      && adduser --uid 1000 --home /home/jenkins --shell /bin/sh --disabled-password --disabled-login jenkins \
      && chown -R jenkins: /home/jenkins \
      && apt update && apt install zip && apt clean

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/
WORKDIR /home/jenkins
USER jenkins


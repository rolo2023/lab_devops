#!/usr/bin/env bash

MAVEN_VERSION=3.0.4
OPENJDK_VERSION=7
NAME="mvn-${MAVEN_VERSION}-jdk-${OPENJDK_VERSION}"
IMAGE_NAME="globaldevtools.bbva.com:5000/piaas/${NAME}:1.0.0"
IMAGES_DIR="images"
DOCKERFILE_NAME="${IMAGES_DIR}/Dockerfile-${NAME}.alpine"

mkdir -p "${IMAGES_DIR}"
cat > "${DOCKERFILE_NAME}" << EOF
FROM openjdk:${OPENJDK_VERSION}-alpine

USER root
RUN set -ex && \
  apk add --no-cache curl git openssh && \
  curl https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xvz -C /usr/share/
RUN set -ex &&  \
  deluser jenkins | true &&  \
  adduser -u 1000 -D  jenkins

ENV PATH=/usr/share/apache-maven-${MAVEN_VERSION}/bin:\$PATH

USER jenkins

EOF
docker build -t "${IMAGE_NAME}" -f "${DOCKERFILE_NAME}" .
docker push "${IMAGE_NAME}"

POD_TEMPLATE_NAME="pod_template-${NAME}.yml"
cat > "${POD_TEMPLATE_NAME}" << EOF
---
# Automatically generated Pod template for Maven ${MAVEN_VERSION} and openjdk ${OPENJDK_VERSION}
apiVersion: "v1"
kind: "Pod"
spec:
  securityContext:
    runAsUser: 1000
    fsGroup: 1000
  imagePullSecrets:
    - name: "registrypullsecret"
  containers:
    - name: "maven-builder"
      image: "${IMAGE_NAME}"
      tty: true
      resources:
        limits:
          memory: 2Gi
          cpu: 2
        requests:
          memory: 2Gi
          cpu: 2
      command:
        - cat
EOF

git add "${DOCKERFILE_NAME}" "${POD_TEMPLATE_NAME}"
git commit -m "Added Dockerfile and Pod template definitior for Maven ${MAVEN_VERSION} and OpenJDK ${OPENJDK_VERSION}"

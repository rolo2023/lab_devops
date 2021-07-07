#!/bin/bash
# This script must be run inside a withCredentials block

HOME_DIR=`pwd`
ROOT_PATH="./enet/online/co/web/1.6/enet_co_web.ear/enet_co_web_war.war/WEB-INF/lib/"
PACKAGE=${PACKAGE:-1}

GIT_SSH_COMMAND="ssh -i ${SSHKEY} -o StrictHostKeyChecking=no" git fetch ssh://git@globaldevtools.bbva.com:7999/bbvaconc/enet.git master:master

varCommand="$(git diff --name-only master... -- $ROOT_PATH/)"
for javaFile in `$varCommand`
do
  echo "Compiling $javaFile"
  javac ${javaFile}
done

varCommand="find $ROOT_PATH/* -maxdepth 0 -type d"
for jarDir in `$varCommand`
do
  jar cvf ${jarDir}.jar $(find ./${jarDir} \( -name "*.java" -o -name "*.class" \)) > /dev/null 2>&1
  rm -Rvf ${jarDir} > /dev/null 2>&1
done

mkdir artifacts
echo "***"
ls -la ./artifacts
echo "${PACKAGE}"
zip -r -q  artifacts/${PACKAGE} ./bdfv ./enet

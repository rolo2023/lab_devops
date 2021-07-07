#!/usr/bin/env bash
#
# create_var
#
# Creates a new, empty and callable module in the 'vars' directory from a blueprint.
#
# Parameters:
# --name|-n     - New module name -better if written in camelCase.
# --test|-t     - Auto-run tests after creation
# --help|-h     - Shows this message

set -e

TOPDIR=$(cd "$(dirname "$0")/../.." && pwd)

error() {
  echo "[ERROR] $*"
  exit 1
}

help() {
  cat << EOF
create_var

Creates a new, empty and callable module in the 'vars' directory from a blueprint.

Parameters:
--name|-n     - New module name -better if written in camelCase.
--test|-t     - Auto-run tests after creation
--help|-h     - Shows this message

EOF
  exit 0
}

while (( "$#" )) ; do
  case $1 in
    --name|-n)
      [ ! -z "$2" ] && shift && MODULE_NAME=$1 ;;
    --test|-t)
      AUTO_RUN_TEST=1 ;;
    --help|-h) help ;;
  esac
  shift
done

[ -z "${MODULE_NAME}" ] && \
  help && error "Missing module name"

# All templates to be found here
SAMPLES_DIR="${TOPDIR}/doc/samples/vars"

# Final file names
DOC_MODULE_NAME="${TOPDIR}/doc/vars/${MODULE_NAME}.md"
[ -f "${DOC_MODULE_NAME}" ] && \
  error "${DOC_MODULE_NAME} already exists -won't overwrite."
sed -e "s/#MODULE_NAME#/${MODULE_NAME}/g" "${SAMPLES_DIR}/doc.sample"   > "${DOC_MODULE_NAME}"

VAR_MODULE_NAME="${TOPDIR}/vars/${MODULE_NAME}.groovy"
[ -f "${VAR_MODULE_NAME}" ] && \
  error "${VAR_MODULE_NAME} already exists -won't overwrite."
sed -e "s/#MODULE_NAME#/${MODULE_NAME}/g" "${SAMPLES_DIR}/module.sample" > "${VAR_MODULE_NAME}"

TEST_MODULE_NAME="${TOPDIR}/test/workflow/vars/${MODULE_NAME}Test.groovy"
[ -f "${TEST_MODULE_NAME}" ] && \
  error "${TEST_MODULE_NAME} already exists -won't overwrite."
sed -e "s/#MODULE_NAME#/${MODULE_NAME}/g" "${SAMPLES_DIR}/test.sample"   > "${TEST_MODULE_NAME}"

cat << EOF
All done!!

- New module has been created in ${VAR_MODULE_NAME}
- Tests placed in ${TEST_MODULE_NAME}
- Documentation can be found in ${DOC_MODULE_NAME}

EOF

if [ ! -z "${AUTO_RUN_TEST}" ] ; then
  TEST_COMMAND="./gradlew test --info --tests workflow.vars.${MODULE_NAME}Test"
  echo "- Running tests using command: ${TEST_COMMAND}" && echo
  command ${TEST_COMMAND}
fi

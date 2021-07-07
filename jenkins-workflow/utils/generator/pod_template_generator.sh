#!/usr/bin/env bash

function help() {
  cat << EOF

Create a Kubernetes Pod Template for each Mesos agent template found in input file.

Usage:

  ${0} -f TEMPLATE_FILE_NAME [ -o OUTPUT_DIR ]

Arguments:
--templates-file|-f   Input file name
--output-dir|-o       Optional output dir for generated files.

To generate the appropiate input file:
* Get jq binary installed. See https://stedolan.github.io/jq/ for more info.
* Go to your JE Mesos instance and download jenkins-cli.jar from the Management menu, Jenkins CLI section.
  (e.g https://globaldevtools.bbva.com/je-mm-gl-adoption-team/cli/ )
* Create an API Token for an user with ADMIN rights, from your profile section
  (e.g https://globaldevtools.bbva.com/je-mm-gl-adoption-team/user/juan.arias.freire/configure)
* Go to the folder where you downloaded the CLI file, and type the following

 java -jar jenkins-cli.jar  \
  -s JENKINS_URL -auth JENKINS_USER:JENKINS_API_KEY agent-template | jq . > my_agent_templates.json

EOF

}

function __error() {
  echo "[ERROR] $*"
  help && exit 1
}

# Parses input arguments and validates them
function parse_args() {
  # Do not even try if there is no jq
  ! command -v "jq" >/dev/null && __error "jq binary not found in PATH"

  # Parse input parameters
  while (("$#")); do
    case $1 in
    --templates-file | -f)
      [ ! -z "$2" ] && shift && TEMPLATES_FILE=$1
      ;;
    --output-dir | -o)
      [ ! -z "$2" ] && shift && OUTDIR=$1
      ;;
    --help | -h)
        help && exit 0
    ;;
    esac
    shift
  done

  if [ -z "${TEMPLATES_FILE}" ]; then
    __error "A template file is needed as input"
  elif [ ! -f "${TEMPLATES_FILE}" ]; then
    __error "${TEMPLATES_FILE} is not a valid file"
  fi
}

export OUTDIR="${PWD}/generated_pod_templates"
export TEMPLATES_FILE=""

function main() {
  parse_args "$@"

  # Create output dir and clean old files there
  mkdir -p "${OUTDIR}" && rm "${OUTDIR}/*.yml"

  for d in $(jq -c '.data[].atm | {"name":.name, "image":.image, "cpu":.cpus, "memory":.memory}' "${TEMPLATES_FILE}"); do

    # Container Names MUST match RFC 1123 - They can only contain lowercase letters, numbers or dashes
    name=$(echo "${d}" | jq .name | tr -d '"' | tr '.' '-' | tr '_' '-' | tr [[:upper:]] [[:lower:]])
    image=$(echo "${d}" | jq .image | tr -d '"')

    # Really simple algorithm.... this can always me revised later!
    memory=$(echo "${d}" | jq .memory | tr -d '"')
    if [ $memory -gt 4000 ] ; then
      memory="4Gi"
      cpu="4"
    elif [ $memory -gt 2000 ] ; then
      memory="2Gi"
      cpu=2
    else
      memory="1Gi"
      cpu=1
    fi

    pod_file_name="${OUTDIR}/pod_template_${name}.yml"
    cat >"${pod_file_name}" <<EOF
---
# Automatically created from a Mesos Agent template definition
apiVersion: "v1"
kind: "Pod"
metadata:
  name: "${name}"
spec:
  securityContext:
    runAsUser: 1000
    fsGroup: 1000
  imagePullSecrets:
    - name: "registrypullsecret"
  containers:
    - name: "${name}"
      image: "${image}"
      tty: true
      resources:
        limits:
          memory: ${memory}
          cpu: ${cpu}
        requests:
          memory: ${memory}
          cpu: ${cpu}
      command:
        - cat
EOF
    echo "Generated ${pod_file_name}"
  done
}

main "$@"

#!/bin/sh

## Get test workspace directory
HERE=$(dirname $0)
if [ $HERE = '.' ]; then HERE=$(dirname $PWD); else HERE=$(dirname $HERE); fi

## Run tests
if [ $# -gt 0 ]; then PARAMS="$@"; else PARAMS='--console=rich --fail-fast -i -t'; fi # No params means default params

# Sub-shell so it doesn't change directory
(cd $HERE && ./gradlew test $PARAMS)

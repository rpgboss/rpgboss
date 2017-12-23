#!/bin/sh

# This script is licensed under Creative Commons Zero (CC0).

ROOT_DIR=$(dirname $(dirname $0))

cd "${ROOT_DIR}/core/src/main/resources/defaultrc"

IFS_orig="${IFS}"
IFS=$'\n'

rm -f "enumerated.txt"

for FILE in $(ls); do
  if [ -f "${FILE}" ]; then
    echo "${FILE}" >> "enumerated.txt"
  fi
done

for DIR in $(ls); do
  if [ -d "${DIR}" ]; then
    for FILE in $(find "${DIR}" -type f ! -name ".*" ! -name "*.xcf" ! -name "*.psd"); do
      echo "${FILE}" >> "enumerated.txt"
    done
  fi
done

IFS="${IFS_orig}"

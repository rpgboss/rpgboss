#!/bin/bash

# This script is licensed under Creative Commons Zero (CC0).

ROOT_DIR=$(dirname $(dirname $0))

cd "${ROOT_DIR}/core/src/main/resources/defaultrc"

rm -f "enumerated.txt"

echo -e "\nDir:\t."
for FILE in *; do
  if [ -f "${FILE}" ]; then
    echo -e "Adding file:\t${FILE}"
    echo "${FILE}" >> "enumerated.txt"
  fi
done

for DIR in *; do
  if [ -d "${DIR}" ]; then
    echo -e "\nDir:\t${DIR}"
    for FILE in $(find "${DIR}" -type f ! -name ".*" ! -name "*.xcf" ! -name "*.psd" ! -name "*.svg"); do
      echo -e "Adding file:\t${FILE}"
      echo "${FILE}" >> "enumerated.txt"
    done
  fi
done

#!/bin/sh
RPGBOSS="$(dirname "$0")"/editor-min.jar
if [ -n "$JAVA_HOME" ]; then
  $JAVA_HOME/bin/java -jar "$RPGBOSS" "$@"
else
  java -jar "$RPGBOSS" "$@"
fi

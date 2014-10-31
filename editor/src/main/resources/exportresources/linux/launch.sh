#!/bin/sh
RPGBOSS="$(dirname "$0")"/rpgboss-library.jar
if [ -n "$JAVA_HOME" ]; then
  $JAVA_HOME/bin/java -jar "$RPGBOSS" --player gamedata "$@"
else
  java -jar "$RPGBOSS" --player gamedata "$@"
fi

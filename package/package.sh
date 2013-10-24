#!/bin/sh
export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

sbt 'project editor' proguard &&
$SRC_DIR/win/package.sh &&
$SRC_DIR/linux/package.sh

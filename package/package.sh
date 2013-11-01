#!/bin/sh
export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

cd $SRC_DIR/..
sbt 'project editor' clean proguard &&
$SRC_DIR/win/package.sh &&
$SRC_DIR/linux/package.sh

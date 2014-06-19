#!/bin/sh
set -e

export VERSION="0.1-SNAPSHOT"
export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

cd $SRC_DIR/..
sbt 'project editor' proguard
$SRC_DIR/win/package.sh
$SRC_DIR/linux/package.sh

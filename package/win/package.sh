#!/bin/sh
echo "Packaging for Windows..."
echo "========================"

rm -rfv $SRC_DIR/target
mkdir $SRC_DIR/target/

export SRC_DIR=$(cd "$(dirname "$0")"; pwd)
launch4j $SRC_DIR/config-win.xml 
cp -v $SRC_DIR/target/* $SRC_DIR/../target/
echo ""

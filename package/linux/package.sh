#!/bin/sh
echo "Packaging for Linux..."
echo "======================"

export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

rm -v $SRC_DIR/target/*
mkdir $SRC_DIR/target
export JARPATH=$SRC_DIR/../../editor/target/scala-2.10/editor_2.10-0.1.min.jar

cp -v $JARPATH $SRC_DIR/target/editor-min.jar
cp -v $SRC_DIR/icon.png $SRC_DIR/rpgboss.desktop $SRC_DIR/target
cp -v $SRC_DIR/launch.sh $SRC_DIR/target/rpgboss

chmod -v 755 $SRC_DIR/target/rpgboss

export ARCHIVE="rpgboss-editor-linux.tar.gz"

tar -cvzf $SRC_DIR/target/$ARCHIVE -C $SRC_DIR/target .

cp -v $SRC_DIR/target/$ARCHIVE $SRC_DIR/../target/

echo ""

#!/bin/sh
set -v
echo "Packaging for Linux/Mac..."
echo "======================"

export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

rm -rfv $SRC_DIR/target/*
export ARCHIVE_PATH=$SRC_DIR/target/rpgboss-$VERSION
mkdir -p $ARCHIVE_PATH
export JARPATH=$SRC_DIR/../../editor/target/scala-2.11/editor-assembly-0.1.jar

cp -v $JARPATH $ARCHIVE_PATH/editor-min.jar
cp -v $SRC_DIR/icon.png $ARCHIVE_PATH
cp -v $SRC_DIR/rpgboss.desktop $ARCHIVE_PATH
cp -v $SRC_DIR/launch.sh $ARCHIVE_PATH/rpgboss

chmod -v 755 $ARCHIVE_PATH/rpgboss

export ARCHIVE="rpgboss-${VERSION}.zip"

cd $SRC_DIR/target
zip -r $ARCHIVE rpgboss-$VERSION

cp -v $SRC_DIR/target/$ARCHIVE $SRC_DIR/../target/rpgboss-$VERSION-linux-mac.zip

echo ""

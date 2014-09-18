#!/bin/sh
echo "Packaging for Linux..."
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

export ARCHIVE="rpgboss-${VERSION}.tar.gz"

tar -cvzf $SRC_DIR/target/$ARCHIVE -C $SRC_DIR/target rpgboss-$VERSION

cp -v $SRC_DIR/target/$ARCHIVE $SRC_DIR/../target/

echo ""

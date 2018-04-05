#!/bin/sh
echo "Packaging for Windows..."
echo "========================"

export SRC_DIR=$(cd "$(dirname "$0")"; pwd)
export JSIGN_VER=2.0
export JSIGN=jsign-$JSIGN_VER.jar
export JSIGN_URL=https://github.com/ebourg/jsign/releases/download/$JSIGN_VER/$JSIGN

rm -rfv $SRC_DIR/target
mkdir $SRC_DIR/target/

launch4j $SRC_DIR/config-win.xml
launch4j $SRC_DIR/config-win-player.xml

cd $SRC_DIR/target

if [ -f "$SRC_DIR/../../keystore.jks" ]; then
	wget $JSIGN_URL
	java -jar $JSIGN --keystore $SRC_DIR/../../keystore.jks --storepass password --alias selfsigned --name "rpgboss" --url rpgboss.com rpgboss-editor.exe
else
	echo "WARNING: keystore.jks not found, executable will not be signed"
fi

export ARCHIVE="rpgboss-${VERSION}.zip"
export ARCHIVE_PATH=$SRC_DIR/target/rpgboss-$VERSION

mkdir -p $ARCHIVE_PATH
cp -v rpgboss-editor.exe $ARCHIVE_PATH/rpgboss-$VERSION.exe

if [ ! -d "$ARCHIVE_PATH/docs" ]; then
	mkdir -p $ARCHIVE_PATH/docs
fi

cp -v $SRC_DIR/../../LICENSE.txt $ARCHIVE_PATH/docs

cd $SRC_DIR/target
zip -r $ARCHIVE rpgboss-$VERSION

cp -v $SRC_DIR/target/$ARCHIVE $SRC_DIR/../target/rpgboss-$VERSION-win.zip

echo ""

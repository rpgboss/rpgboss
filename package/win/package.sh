#!/bin/sh
echo "Packaging for Windows..."
echo "========================"

export SRC_DIR=$(cd "$(dirname "$0")"; pwd)

rm -rfv $SRC_DIR/target
mkdir $SRC_DIR/target/

launch4j $SRC_DIR/config-win.xml 

#cd $SRC_DIR/target
#wget http://people.apache.org/~ebourg/jsign/jsign-1.2.jar
#java -jar jsign-1.2.jar --keystore $SRC_DIR/../../keystore.jks --storepass password --alias selfsigned --name "rpgboss" --url rpgboss.com rpgboss-editor.exe

cp -v $SRC_DIR/target/rpgboss-editor.exe $SRC_DIR/../target/rpgboss-${VERSION}.exe
echo ""

#!/bin/bash
VERSION="2.0.6-SNAPSHOT"
BUILD="1"
JAR="./dist/jrrd2-api-$VERSION.jar"
LIB="./dist/libjrrd2.so"

if [ ! -e build-packages.sh ]; then
  echo "build-packages.sh must be ran from the root of the project."
fi

# Cleanup
rm -f ./dist/*.rpm
rm -f ./dist/*.deb
rm -rf ./tmp && mkdir ./tmp
mkdir -p ./dist

# Make sure fpm is installed before proceeding
which fpm >/dev/null
if [ $? -ne 0 ]; then
  echo "The fpm command is not available. See https://github.com/jordansissel/fpm for installation instructions."
fi

function createPackage() {
  TYPE="$1"
  EXTRA_ARGUMENTS="$2"
  fpm -s dir \
      --name jrrd2 \
      --description "A native interface to rrdtool for Java" \
      --vendor "OpenNMS" \
      --license "GPLv2" \
      --maintainer "opennms@opennms.org" \
      --url "https://github.com/OpenNMS/jrrd2" \
      --version $VERSION \
      -t $TYPE \
      -C ./tmp/$TYPE \
      -d "rrdtool > 1.5.0" \
      -p ./dist $EXTRA_ARGUMENTS
}

# Build the tree for the .rpm package
mkdir -p ./tmp/rpm/usr/lib64
mkdir -p ./tmp/rpm/usr/share/java
cp $LIB ./tmp/rpm/usr/lib64/libjrrd2.so
chmod 755 ./tmp/rpm/usr/lib64/libjrrd2.so
cp $JAR ./tmp/rpm/usr/share/java/jrrd2.jar
chmod 644 ./tmp/rpm/usr/share/java/jrrd2.jar

createPackage "rpm" "--epoch $BUILD" || exit 1

# Build the tree for the .deb package
mkdir -p ./tmp/deb/usr/lib/jni
mkdir -p ./tmp/deb/usr/share/java
cp $LIB ./tmp/deb/usr/lib/jni/libjrrd2.so
chmod 755 ./tmp/deb/usr/lib/jni/libjrrd2.so
cp $JAR ./tmp/deb/usr/share/java/jrrd2.jar
chmod 644 ./tmp/deb/usr/share/java/jrrd2.jar

createPackage "deb" "--iteration $BUILD" || exit 1


#!/bin/bash

if [ ! -e build.sh ]; then
  echo "build.sh must be ran from the root of the project."
  exit 1
fi

which cmake >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "You must install cmake first."
  exit 1
fi

which mvn >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "You must install maven first."
  exit 1
fi

set -e

if [ -z "${JAVA_HOME}" ]; then
	for DIR in /usr/lib/jvm/*1.8* /usr/lib/jvm/java-7-openjdk-*; do
		if [ -d "$DIR" ] && [ -x "$DIR"/bin/javac ]; then
			export JAVA_HOME="$DIR"
		fi
	done
fi
export PATH="$JAVA_HOME/bin:$PATH"

# Cleanup
pushd . && rm -rf build dist && mkdir build

# Generate the JNI interface header
cd ./java
mvn clean compile

declare -a CMAKE_ARGS=()
if [ -n "${CMAKE_OSX_ARCHITECTURES}" ]; then
	CMAKE_ARGS+=("-DCMAKE_OSX_ARCHITECTURES=${CMAKE_OSX_ARCHITECTURES}")
fi

# Build the shared library
cd ../build
cmake "${CMAKE_ARGS[@]}" ../jni/
make

# Run the tests and create the .jar
cd ../java
mvn package || (echo "Build failed." && exit 1)
cp target/jrrd2-api-*.jar ../dist/

popd

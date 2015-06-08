#!/bin/bash

if [ ! -e build.sh ]; then
  echo "build.sh must be ran from the root of the project."
fi

# Cleanup
pushd . && rm -rf build dist && mkdir build

# Generate the JNI interface header
cd ./java
mvn compile

# Build the shared library
cd ../build
cmake ../jni/
make

# Run the tests and create the .jar
cd ../java
mvn package || (echo "Build failed." && exit 1)
cp target/jrrd2-api-*.jar ../dist/

popd

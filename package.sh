#!/bin/sh
# run this after: mvn package
rm -Rf dist
mkdir -p dist/src/main/resources
mv target/spimedb-1.0.one-jar.jar dist/
cp -R src/main/resources/public dist/src/main/resources

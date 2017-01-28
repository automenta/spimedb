#!/bin/bash

# Requirements:
#	git
#	openjdk9
#	maven
#	bower


echo 'Building SpimeDB...'
echo '-------------------'
echo

echo 'Build JCog'
git clone --depth 1 https://github.com/automenta/narchy/
pushd .
	cd narchy/util
	mvn install -DskipTests=true
popd



echo 'Build SpimeDB'
mvn install -DskipTests=true


echo 'Generate Web App'
pushd .
	cd src/main/resources/public/
	bower install
	bower update
popd

echo 'Finished'


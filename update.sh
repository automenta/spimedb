#!/bin/bash

# Requirements:
#	git
#	openjdk9
#	maven
#	bower


echo 'Building SpimeDB...'
echo '-------------------'
echo

echo 'Updating from git'
git pull

echo 'Update dependency: JCog'
git clone --depth 1 https://github.com/automenta/narchy/ 2> /dev/null # just to be sure
pushd .
    cd narchy
    git pull
popd

echo 'Build dependency: JCog'
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


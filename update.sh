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
[[ $CLEAN ]] && rm -fr ~/.m2/repository/automenta  ~/.m2/repository/opennars  narchy

git clone --depth 1 https://github.com/automenta/narchy 2> /dev/null # just to be sure
pushd .
    cd narchy
    git pull||exit 1
popd

echo 'Build dependency: JCog'
pushd .


	cd narchy/util
	mvn install -DskipTests=true||exit 1
popd


echo 'Build SpimeDB'

[[ $CLEAN ]] && mvn clean
mvn install -DskipTests=true||exit 1


echo 'Generate Web App'
pushd .
	cd src/main/resources/public/
	bower install||exit 1
	bower update||exit 1
popd


echo 'Finished'


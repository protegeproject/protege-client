#!/bin/bash
# 
# This script clones and builds the repositories related to the Protege client-server as 
# well as Lucene search. The repository cloning step can be skipped by providing some 
# non-empty argument to this script.
#


#################################################################
# Variables
#################################################################

# URL of GitHub account, relative to which all GitHub repositories are resolved
GITHUB_ACCOUNT=https://github.com/protegeproject

# workspace folder, relative to which all repositories below are resolved
WORKSPACE=`pwd`

# Protege version
PROTEGE_VERSION=5.0.1-SNAPSHOT

# Protege repository folder
PROTEGE_REPO=$WORKSPACE/protege

# Protege distribution folder
PROTEGE=$PROTEGE_REPO/protege-desktop/target/protege-$PROTEGE_VERSION-platform-independent/Protege-$PROTEGE_VERSION

# client-server repositories
CLIENT=$WORKSPACE/protege-client
SERVER=$WORKSPACE/protege-server
METAPROJECT=$WORKSPACE/metaproject
ADMIN_TAB=$WORKSPACE/protege-server-admin-tab

# Lucene repositories
LUCENE=$WORKSPACE/lucene-search-plugin
LUCENE_TAB=$WORKSPACE/lucene-search-tab

# Export plugin repository
EXPORT_PLUGIN=$WORKSPACE/csv-export-plugin



#################################################################
# Functions
#################################################################

# function to clone a repository. Takes as arguments: [repository-name] [git-branch-name]
clone() { 
	echo
	echo "cloning repository '$GITHUB_ACCOUNT/$1'"
	echo
	git clone $GITHUB_ACCOUNT/$1.git $WORKSPACE/$1
	
	# checkout the given branch
	if [ ! -z $2 ]; then
		echo "changing to branch '$2'"
		cd $WORKSPACE/$1
		git checkout $2
	fi
}


# function that takes a path to a repository and builds it
build() {
	echo
	echo "building repository '$1'..."
	echo
	cd $1
	mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dsource.skip=true
}


# function that takes a path to a repository, runs build() with it, 
# and copies the resulting JAR to Protege's plugins folder
buildPlugin() {
	build $1
	cp target/*.jar ${PROTEGE}/plugins
	rm -rf $1
}



#################################################################
# Clone repositories
#################################################################

# repository cloning can be skipped by providing some argument to the script
if [ -z "$1" ]; then
	echo "cloning repositories..."

	clone protege search-api
	clone protege-server http-metaproject-integration
	clone protege-client http-metaproject-integration
	clone metaproject
	clone protege-server-admin-tab
	clone lucene-search-plugin
	clone lucene-search-tab
	clone csv-export-plugin
	
	echo "done cloning repositories"
fi



#################################################################
# Build repositories
#################################################################

echo
echo "building repositories..."

build $PROTEGE_REPO

# create Protege plugins folder if it does not exist
PLUGINS_DIR=${PROTEGE}/plugins
if [ ! -d "$PLUGINS_DIR" ]; then
  mkdir "$PLUGINS_DIR"
fi

# build client-server repositories
buildPlugin $METAPROJECT
buildPlugin $SERVER
buildPlugin $CLIENT
buildPlugin $ADMIN_TAB

# build export plugin
buildPlugin $EXPORT_PLUGIN

# build lucene repositories
buildPlugin $LUCENE
buildPlugin $LUCENE_TAB

echo
echo "done building repositories"



#################################################################
# Run Protege
#################################################################

mv $PROTEGE $WORKSPACE
rm -rf $PROTEGE_REPO

echo "running Protege..."
echo
cd $WORKSPACE/Protege-$PROTEGE_VERSION && ./run.sh

#!/bin/bash

##
# Bash script to automate the ant release of FITS, for
# testing.
#
# author Carl Wilson carl@openplanetsfoundation.org
#
# Invokes:
#  
#  * ant clean
#  * ant compile
#  * ant release
#
# Script can takes 1 parameter:
#
#  $1 directory for FITS project
#     defaults to current directory
##

ANT_SUCCESSFUL="SUCCESSFUL"
RELEASE_DIR=".release"
buildDir="."
# Check the passed params to avoid disapointment
checkParams () {
	# If we have params
	if [[ "$#" -gt 0 ]]
	then
		# Check that the build directory exists
		if  [[ ! -d "$1" ]]
		then
			echo "Build directory not found: $1"
			exit 1;
		fi
		buildDir="$1"
	fi
}

# Build the FITS project invoking ant tasks
buildFits() {
	cd "$buildDir"
	antclean=$(ant clean 2>&1)
	echo "${PWD##*/}: ant clean"
	testAntCommand "$antclean"
 
	antCompile=$(ant compile 2>&1)
	echo "${PWD##*/}: ant compile"
	testAntCommand "$antCompile"
	
	echo "${PWD##*/}: ant release"
	if [[ -d "$RELEASE_DIR" ]]
	then
		rm -rf "$RELEASE_DIR"
	fi
	antRelease=$(ant release <<< "$RELEASE_DIR" 2>&1)
	testAntCommand "$antRelease"
}

# Run an ant command and test the status, exit if fails
testAntCommand() {
	antCommand=$1
	wasAntSuccessful "$antCommand"
	antStatus=$?
	if (( ! $antStatus == 0 ))
	then
		echo "$antCommand"
		exit antStatus;
	fi
}

# Run an ant command and parse the output for the build status
wasAntSuccessful() {
	antCmd=$1
	buildRegEx="^BUILD ([A-Z]*)$"
	antStatus="UNKOWN"
	while IFS= read -r
	do
		if [[ $REPLY =~ $buildRegEx ]]
		then
			antStatus="${BASH_REMATCH[1]}"
		fi
	done <<< "$antCmd"

	if [[ $antStatus == $ANT_SUCCESSFUL ]]
	then 
		return 0;
	fi
	
	return 1;
}

unpackFits() {
	# Find the FITS release .zip
	cd $RELEASE_DIR
	findzip=$(find . -name "fits*.zip" -type f 2>&1)
	if [[ ! -e "$findzip" ]]
	then
		echo "Couldn't find FITS release zip."
		exit 1;
	fi
	unziprelease=$(unzip "$findzip")
	rm "$findzip"
}

checkParams "$@";
buildFits;
unpackFits;

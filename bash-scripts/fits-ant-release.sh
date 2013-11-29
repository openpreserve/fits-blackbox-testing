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
# Usage fits-ant-release [-b <path>] [-r <path>] [-h | -?]
# Options:
#  -b <path>  Path to build dir defaults to current dir.
#  -r <path>  Path to release dir, defaults to .bb-testing/release.
#
##

ANT_SUCCESSFUL="SUCCESSFUL"
releaseDir=".bb-testing/release"
buildDir="."
# Check the passed params to avoid disapointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "h?b:r:" opt; do	# Grab the options
		case "$opt" in
		h|\?)
			showHelp
			exit 0
			;;
		b)	buildDir=$OPTARG
			;;
		r)	releaseDir=$OPTARG
			;;
		esac
	done
	shift $((OPTIND-1))

	[ "$1" = "--" ] && shift
	# Check that the buid dir
	if  [[ ! -e "$buildDir" ]]
	then
		echo "FITS Build directory NOT found : $buildDir"
		exit 1;
	fi
}

# Show usage message
showHelp() {
	echo "Usage fits-ant-release [-b <path>] [-r <path>] [-h | -?]"
	echo ""
	echo "Options:"
	echo "  -b <path>  Path to build dir defaults to current dir."
	echo "  -r <path>  Path to release dir, defaults to .bb-testing/release."
	echo "  -h | -?    show this message."
}

# Build the FITS project invoking ant tasks
buildFits() {
	cd "$buildDir"
	echo "Building ${PWD##*/}"
	antclean=$(ant clean 2>&1)
	testAntCommand "$antclean"
 
	antCompile=$(ant compile 2>&1)
	testAntCommand "$antCompile"
	
	if [[ -d "$releaseDir" ]]
	then
		rm -rf "$releaseDir"
	fi
	antRelease=$(ant release <<< "$releaseDir" 2>&1)
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
	cd $releaseDir
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

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
# Usage fits-build-and-execute [-b <path>] [-c <path>] [-o <path>] [-h | -?]
# Options:
#  -b <path>  Path to build dir defaults to current dir.
#  -c <path>  Path to corpora dir, defaults to .corpora
#  -o <path>  Path to output location root defaults to /tmp/fits/bb-testing
#  -h | -?    show this message.
#
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

DEFAULT_ROOT="/tmp/fits/bb-testing"

buildDir="."
corporaDir=".corpora"
outRoot="$DEFAULT_ROOT"
fitsReleaseDir="release"
fitsOutputDir="output"
# Check the passed params to avoid disapointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "h?b:c:o:" opt; do	# Grab the options
		case "$opt" in
		h|\?)
			showHelp
			exit 0
			;;
		b)	buildDir=$OPTARG
			;;
		c)	corporaDir=$OPTARG
			;;
		o)	outRoot=$OPTARG
			;;
		esac
	done
	shift $((OPTIND-1))

	[ "$1" = "--" ] && shift
	# Check that the buid dir exists
	if  [[ ! -e "$buildDir" ]]
	then
		echo "FITS Build directory NOT found : $buildDir"
		exit 1;
	fi
	# Check that the corpora dir exists
	if  [[ ! -e "$corporaDir" ]]
	then
		echo "Corpora directory NOT found : $corporaDir"
		exit 1;
	fi
	# Set the release dir
	fitsReleaseDir="$outRoot/$fitsReleaseDir"
}

# Show usage message
showHelp() {
	echo "Usage fits-build-and-execute [-b <path>] [-c <path>] [-o <path>] [-h | -?]"
	echo ""
	echo "Options:"
	echo "  -b <path>  Path to build dir defaults to current dir."
	echo "  -c <path>  Path to corpora dir, defaults to .corpora/ ."
	echo "  -o <path>  Path to output location root, defaults to /tmp/fits/bb-testing/ ."
	echo "  -h | -?    show this message."
}

# Invoke the ant-release script, exit on failure
buildFits() {
	bash "$SCRIPT_DIR/fits-ant-release.sh" -r "$fitsReleaseDir"
	if (( $? != 0))
	then
		exit $?;
	fi
}

# Find the unzipped release directory
findRelease() {
	releaseDir=$(find $fitsReleaseDir -name "fits-*" -type d 2>&1)
	if [[ ! -d "$releaseDir" ]]
	then
		echo "FITS release NOT found."
		echo "$releaseDir"
		exit 1;
	fi
}

# Setup output directory and execute FITS
executeFits() {
	bash "$SCRIPT_DIR/execute-fits.sh" -c "$corporaDir" -f "$releaseDir" -o "$outRoot"
	if (( $? != 0))
	then
		exit $?;
	fi
}

checkParams "$@";
buildFits;
findRelease;
executeFits;

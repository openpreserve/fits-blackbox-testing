#!/bin/bash

##
# Bash script to build and test the current FITS checkout from source, against a master output.
#
# author Carl Wilson carl@openplanetsfoundation.org
#
# usage fits-test-current [-t <path>] [-c <path>] [-s <path>]
# Options:
#  -t <path>  use the FITS testing tool at <path>.
#  -c <path>  run tests on corpora at directory <path>.
#  -s <path>  path to baseline output for testing against.
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

paramToolPath=""
paramCorporaPath=""
paramBaselinePath=""
gitHash=""
# Check the passed params to avoid disapointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "t:c:s:" opt; do	# Grab the options
		case "$opt" in
		t)	paramToolPath=$OPTARG
			;;
		c)	paramCorporaPath=$OPTARG
			;;
		s)	paramBaselinePath=$OPTARG
			;;
		esac
	done
	shift $((OPTIND-1))

	[ "$1" = "--" ] && shift
	# Check that tool exists
	if  [[ ! -e "$paramToolPath" ]]
	then
		echo "Testing tool not found: $paramToolPath"
		exit 125;
	fi
	# Check that corpora exists
	if  [[ ! -e "$paramCorporaPath" ]]
	then
		echo "Corpora root not found: $paramCorporaPath"
		exit 125;
	fi
	# Check that baseline output exists
	if  [[ ! -e "$paramBaselinePath" ]]
	then
		echo "Baseline output root not found: $paramBaselinePath"
		exit 125;
	fi

    gitHash=$(git rev-parse HEAD 2>&1)
	if [[ "x$gitHash" == "x" ]]
	then
    	echo "Git commit cannot be empty"
    	exit 1;
	fi
}

# Invoke the ant-release script, exit on failure
buildFits() {
	bash "$SCRIPT_DIR/fits-ant-release.sh"
	if (( $? != 0))
	then
		resetHead
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
		resetHead
		checkoutCurrentBranch;
		exit 1;
	fi
}

# Setup output directory and execute FITS
executeFits() {
  githash=$1
	outputDir="$fitsOutputDir/$githash"
	if [[ -d "$outputDir" ]]
	then
		rm -rf "$outputDir"
	fi
	mkdir -p "$outputDir"
	bash "$SCRIPT_DIR/execute-fits.sh" -c "$paramCorporaLoc" -f "$releaseDir"
	if (( $? != 0))
	then
		resetHead;
		exit $?;
	fi
}

checkParams
# Build master revision for comparison
echo "TEST:build"
buildFits;
echo "TEST:find"
findRelease;
# Execute FITS sending output to hash named output dir
echo "TEST:execute"
executeFits "$githash";


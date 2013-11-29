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
#  -o <path>  path to root of all output, defaults to /tmp/fits/bb-testing .
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

DEFAULT_ROOT="/tmp/fits/bb-testing"

paramToolPath=""
paramCorporaPath=""
paramBaselinePath=""
outRoot="$DEFAULT_ROOT"
gitHash=""
# Check the passed params to avoid disapointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "t:c:s:o:" opt; do	# Grab the options
		case "$opt" in
		t)	paramToolPath=$OPTARG
			;;
		c)	paramCorporaPath=$OPTARG
			;;
		s)	paramBaselinePath=$OPTARG
			;;
		o)	outRoot=$OPTARG
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
	shaRegEx="^[0-9a-f]{40}$"
	if [[ ! $gitHash =~ $shaRegEx ]]
	then
		echo "Cannot determine current commit hash: $gitHash"
		exit 1;
	fi
	# Set the output dir
	fitsOutputDir="$outRoot/output/$gitHash"
}

# Invoke the checkGitStatus script, exit on failure
buildAndExecuteFits() {
	bash "$SCRIPT_DIR/fits-build-and-execute.sh" -c "$paramCorporaPath" -o "$outRoot"
		if (( $? != 0))
	then
		resetHead
		exit $?;
	fi
}

checkParams "$@";
buildAndExecuteFits
echo "java  -jar $paramToolPath -s $paramBaselinePath -c $fitsOutputDir -k $gitHash"
java  -jar "$paramToolPath" -s "$paramBaselinePath" -c "$fitsOutputDir" -k "$gitHash"
exitStat="$?"
git clean -f
exit $exitStat;

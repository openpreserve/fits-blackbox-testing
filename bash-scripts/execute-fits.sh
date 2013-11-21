#!/bin/bash

##
# Bash script to automate FITS execution for testing.
#
# author Carl Wilson carl@openplanetsfoundation.org
#
# This script checks a directory to ensure that:
#
#  * The directory is a git repo
#  * The repo has no uncommitted changes
#  * That a master branch exists
#  * That a branch other than master is checked out
#
# Script can takes 3 parameters:
#
#  $1 path to test corpora release
#     defaults ./.corpora
#
#  $2 path to output directory
#     defaults ./.output
#
#  $3 path to FITS release
#     defaults ./.release
#  
##

FITS_SHELL="fits.sh"

outputDir=".bb-testing/output"
fitsDir=".bb-testing/release"
corporaDir=".corpora"
# Check the passed params to avoid disappointment
checkParams () {
	# If we have at least 1 param
	if [[ "$#" -gt 0 ]]
	then
		corporaDir="$1"
	fi
	# Check that the corpora directory exists
	if  [[ ! -d "$corporaDir" ]]
	then
		echo "Corpora directory not found: $1"
		exit 1;
	fi
	
	# If we have at least 2 params
	if [[ "$#" -gt 1 ]]
	then
		outputDir="$2"
	fi
	# Check that the output dir exists
	if  [[ ! -d "$outputDir" ]]
	then
		echo "Output directory not found: $2"
		exit 1;
	fi

	# If we have at least 3 params
	if [[ "$#" -gt 2 ]]
	then
		fitsDir="$3"
	fi
	# Check that the FITS release exists
	if  [[ ! -d "$fitsDir" ]]
	then
		echo "FITS release directory not found: $3"
		exit 1;
	fi

}

checkFitsVersion() {
	chmod +x "$fitsDir/$FITS_SHELL"
	fitsver=$($fitsDir/$FITS_SHELL -v)
	versRegEx="^[0-9.]+$"
	if [[ ! $fitsver =~ $verRegEx ]]
	then
		echo "fits version not found"
		echo "$fitsver"
	fi
}

executeFits() {
	echo "Running FITS v. $fitsver ...."
	fitsout=$($fitsDir/$FITS_SHELL -i $corporaDir -o $outputDir -r 2>&1)
	echo "Finished."
}

checkParams "$@";
checkFitsVersion;
executeFits;

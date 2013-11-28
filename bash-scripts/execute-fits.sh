#!/bin/bash

##
# Bash script to automate FITS execution for testing.
#
# author Carl Wilson carl@openplanetsfoundation.org
#
# usage: execute-fits [-f <path>] [-c <path>] [-a <path>]
# Options:
#  -f <path>  path to FITS tool, defaults to ./.bb-testing/release/ .
#  -c <path>  path to test corpora to invoke FITS upon, defaults to ./.corpora/ .
#  -a <path>  path to root of all output, defaults to ./bb-testing/ .
#  
##

FITS_SHELL="fits.sh"

globalOutput=".bb-testing"
fitsDir="ls $globalOutput/release"
fitsOutputDir="output"
fitsLogDir="log"
corporaDir=".corpora"
# Check the passed params to avoid disappointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "f:c:a:" opt; do	# Grab the options
		case "$opt" in
		f)	fitsDir=$OPTARG
			;;
		c)	corporaDir=$OPTARG
			;;
		a)	globalOutput=$OPTARG
			;;
		esac
	done
	shift $((OPTIND-1))
	

	[ "$1" = "--" ] && shift
    gitHash=$(git rev-parse HEAD 2>&1)

	# Check that the corpora directory exists
	if  [[ ! -d "$corporaDir" ]]
	then
		echo "Corpora directory not found: $1"
		exit 1;
	fi

	# Check that the global output directory exists
	if  [[ ! -d "$globalOutput" ]]
	then
		echo "Global output directory not found: $1"
		exit 1;
	fi
	
	# Check that the output dir exists
	fitsOutputDir="$globalOutput/$fitsOutputDir/$gitHash"
	if  [[ ! -d "$fitsOutputDir" ]]
	then
		echo "Output directory not found: $2"
		exit 1;
	fi

	# Check that the FITS release exists
	if  [[ ! -d "$fitsDir" ]]
	then
		echo "FITS release directory not found: $3"
		exit 1;
	fi
  
    fitsLogDir="$globalOutput/$fitsLogDir/$gitHash"
	if [[ "x$gitHash" == "x" ]]
	then
    	echo "Git commit cannot be empty"
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
	fitsout=$($fitsDir/$FITS_SHELL -i $corporaDir -o $fitsOutputDir -r 2>&1)
  echo $fitsout >> "$fitsLogDir/fits_output.log"
	echo "Finished."
}

createLogDir() {
  if [[ ! -d "$fitsLogDir" ]]
  then
    mkdir -p $fitsLogDir
  fi
}

checkParams "$@";
checkFitsVersion;
createLogDir;
executeFits;

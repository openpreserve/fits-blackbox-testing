#!/bin/bash

##
# Bash script to automate the regression testing of FITS.
#
# author Carl Wilson carl@openplanetsfoundation.org
# 
# This initial cut is intended to be run from the root 
# directory of a FITS development branch.  Given the path
# to a FITS testing tool and a path to test corpora the script:
#
#	* Checks that there's no uncommitted code.
#   * Checks that you're not on master.
#	* Builds the current branch and generates test output.
#   * Checks out the master commit that's the source of this branch.
#	* Builds FITS and generates the more test output.
#	* Use the FITS testing tool to compare the output.
#	* If successful, i.e. the output is unchanged report success.
#   * If unsuccessful use git-bisect to find the last good commit.
#
# The FITS testing tool should be a command line application 
# that tests 2 sets of FITS generated output and returns:
#
#  0     If the tests succeed
#
#  1-124 If the tests fail
#
#  125   If the tests cannot be performed
#
# These values are for use with git bisect run command
# https://www.kernel.org/pub/software/scm/git/docs/git-bisect.html
# http://git-scm.com/book/en/Git-Tools-Debugging-with-Git
#
# Script expects 2 parameters:
#
#  $1 path to the FITS testing tool to use
#     Mandatory
#
#  $2 path to root directory of test corpora to use 
#     Mandatory
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Globals to hold the checked param vals
paramFitsToolLoc=
paramCorporaLoc=
resetHead=0
currentBranch=
globalOutput=".bb-testing"
fitsOutputDir="$globalOutput/output"
fitsReleaseDir="$globalOutput/release"
##
# Functions defined first, control flow at the bottom of script
##

# Check the passed params to avoid disapointment
checkParams () {
# Ensure we have the correct number of params
	if [[ "$#" -ne 2 ]]
	then
		echo "$# Two parameters expected: $1"
		exit 1
	fi
	
	paramFitsToolLoc="$1"
	paramCorporaLoc="$2"
	
# Check that the FITS testing tool exists
	if  [[ ! -e "$paramFitsToolLoc" ]]
	then
		echo "FITS Testing tool not found: $paramFitsToolLoc"
		exit 1;
	fi
	
# Check that the corpora directory exists
	if  [[ ! -d "$paramCorporaLoc" ]]
	then
		echo "Corpora directory not found: $paramCorporaLoc"
		exit 1;
	fi
}

# Checks if there is a .bb-testing dir in the current working dir.
# if there is one, it is removed, so that a fresh test can be executed.
wipeOutOldData() {
	if  [[ -d "$globalOutput" ]]
	then
		echo "Old test output data found, removing...: $globalOutput"
    rm -r "$globalOutput/"
	fi
}

# Check we've got a master branch and it's not checked out,
# intended to be run on a development branch
checkMaster() {
	gitbranch=$(git branch 2>&1)
	currentBranchRegEx="^\* (.*)$"
	masterBranchRegEx="^[ ]*master$"
	masterFound=0
	while IFS= read -r
	do
		if [[ $REPLY =~ $currentBranchRegEx ]]
		then
			currentBranch="${BASH_REMATCH[1]}"
		elif [[ $REPLY =~ $masterBranchRegEx ]]
		then
				masterFound=1
		fi
	done <<< "$gitbranch"
	if [[ $currentBranch =~ $masterBranchRegEx ]]
	then
		echo "Current branch is master, please check out another branch."
		exit 1;
	fi
	if (( masterFound == 0 ))
	then
		echo "No master branch found to test against"
	fi
}

# Get the commit hash of the current branch HEAD
getHeadHash() {
	githeadhash=$(git rev-parse HEAD 2>&1)
}

# Find the hash of the current branch's split from master
findMergeBaseHash() {
	gitshow=$(git show --pretty=format:"%H" `git merge-base "$currentBranch" master` 2>&1)
	shaRegEx="^[0-9a-f]{40}$"
	while IFS= read -r
	do 
		if [[ $REPLY =~ $shaRegEx ]]
		then
			mergebasehash=$REPLY
		fi
	done <<< "$gitshow"
	
	if [[ $mergebasehash == $githeadhash ]]
	then
		echo "$currentBranch is a fresh branch and doesn't differ from its master root"
		exit 1
	fi
}

# Checkout a particular revision on the current branch by hash
checkoutRevision() {
	echo "Checking out revision $1"
	gitcheckout=$(git checkout $1) # removing '.' (current directory). Otherwise the changed files are kept and screw up the build
	coOkRegEx="^Checking out revision $1"
	echo "$gitcheckout"
}

# Invoke the checkGitStatus script, exit on failure
checkGitStatus() {
	bash "$SCRIPT_DIR/check-git-status.sh"
	if (( $? != 0))
	then
		resetHead
		exit $?;
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
	bash "$SCRIPT_DIR/execute-fits.sh" "$paramCorporaLoc" "$outputDir" "$releaseDir" "$githash"
	if (( $? != 0))
	then
		resetHead;
    checkoutCurrentBranch;
		exit $?;
	fi
}

# Output warning r.e. current git status, IF warning flag set 
resetHead() {
	if (( $resetHead == 1 ))
	then
		git reset HEAD --hard
	fi
}

# Checks out the starting branch if the the parameter is set.
checkoutCurrentBranch() {
  if [ "x$currentBranch" != "x" ] 
  then
    echo "Current Branch is: $currentBranch"
    checkoutRevision "$currentBranch"
  fi
}


testHeadAgainstMergeBase() {
 	java  -jar "$paramFitsToolLoc" -s "$fitsOutputDir/$mergebasehash" -c "$fitsOutputDir/$githeadhash" -k "$githeadhash"
 	case "$?" in
 		# Test passed so no need to look for broken revision
 		"0" )
 		echo "Test of HEAD against branch base succeeded, no broken revision to find."
 		resetHead   
    checkoutCurrentBranch; 
 		exit 0;
 		;;
 		# Test of dev branch HEAD against master couldn't be performed
		# We probably don't want to go on until tests execute
 		"125" )
 		echo "Test of HEAD against branch base could not be performed"
 		resetHead
    checkoutCurrentBranch
 		exit 1;
 		;;
 		# Test failed, exit for now but the start for the
		# revision that broke the test starts here
 		* )
 		echo "Test of HEAD against branch base failed"
 		resetHead
    checkoutCurrentBranch;
 		exit 1;
 	esac
}

##
# Script Execution Starts HERE 
##

# Check and setup parameters
checkParams "$@";
# Look for a .bb-testing directory and remove it
wipeOutOldData;
# We're in a git repo with no uncommitted changes?
checkGitStatus;
echo "In git repo ${PWD##*/}"
# There's a master branch and it's not checked out?
checkMaster;
echo "Testing branch: $currentBranch"
# Grab the git details
getHeadHash
echo "HEAD $githeadhash"
findMergeBaseHash;
echo "BASE commit $mergebasehash"
# Build current version of FITS
buildFits;
findRelease;
# Execute FITS sending output to hash named output dir
executeFits "$githeadhash";

# Set reset HEAD flag, we're about to check out changes
resetHead=1
resetHead;
checkoutCurrentBranch;
# Checkout master branch base
checkoutRevision "$mergebasehash"
# Build master revision for comparison
buildFits;
findRelease;
# Execute FITS sending output to hash named output dir
executeFits "$mergebasehash";

testHeadAgainstMergeBase;

# Reset repo to head
resetHead;
checkoutCurrentBranch;

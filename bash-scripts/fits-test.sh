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
# usage: fits-test [-t <path>] [-c <path>] [-b] [-h|?]
# Options:
#  -t <path>  use the FITS testing tool at <path>, REQUIRED.
#  -c <path>  run tests on corpora at directory <path>, REQUIRED.
#  -b         invoke git-bisect if test against merge base fails.
#  -h | -?    show this message.
#
# SEE helpOut function for full message.
#
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Globals to hold the checked param vals
paramFitsToolLoc=""
paramCorporaLoc=""
paramGitBisect=0
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
	OPTIND=1	# Reset in case getopts previously used

	while getopts "h?b?t:c:" opt; do	# Grab the options
		case "$opt" in
		h|\?)
			showHelp
			exit 0
			;;
		b)	paramGitBisect=1
			;;
		t)	paramFitsToolLoc=$OPTARG
			;;
		c)	paramCorporaLoc=$OPTARG
			;;
		esac
	done
	shift $((OPTIND-1))

	[ "$1" = "--" ] && shift
		
	if [ -z "$paramFitsToolLoc" ] || [ -z "$paramCorporaLoc" ]
	then
		showHelp
		exit 0
	fi
		
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

# Show usage message
showHelp() {
	echo "usage: fits-test [-t <path>] [-c <path>] [-b] [-h|?]"
	echo ""
	echo "Should be run from a FITS git repository, from a checked out development branch."
	echo "The script first locates the current branch BASE, its root in master."
	echo "This version of FITS is checked out, built and executed against the corpora.
	echo "Its output XML files and stdout/stderr are preserved for testing."
	echo "Next current HEAD is checked out, built and executed against the corpora."
	echo "Its output XML files and stdout/stderr are preserved for testing."
	echo "Next the test tool is used to compare HEADs output with that of merge BASE."
	echo "If the tests pass or fail to run, or the -b is not set the script reports and terminates.
	echo "If the tests fail and -b is set then git-bisect will be used to test the"
	echo "branch history until the commit that first caused the test to fail is located."
	echo "" 
	echo "Options:"
	echo "  -t <path>  use the FITS testing tool at <path>, REQUIRED."
	echo "  -c <path>  run tests on corpora at directory <path>, REQUIRED."
	echo "  -b         invoke git-bisect if test against merge base fails."
	echo "  -h | -?    show this message."
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

# Output warning r.e. current git status, IF warning flag set 
resetHead() {
	if (( $resetHead == 1 ))
	then
		git reset HEAD --hard
		resetHead=0
	fi
	checkoutCurrentBranch;
}

# Checks out the starting branch if the the parameter is set.
checkoutCurrentBranch() {
  if [ "x$currentBranch" != "x" ] 
  then
    echo "Current Branch is: $currentBranch"
    checkoutRevision "$currentBranch"
  fi
}

# Invoke the checkGitStatus script, exit on failure
buildAndExecuteFits() {
	bash "$SCRIPT_DIR/fits-build-and-execute.sh" -c "$paramCorporaLoc" -o "$globalOutput"
	if (( $? != 0))
	then
		resetHead
		exit $?;
	fi
}

testHeadAgainstMergeBase() {
 	java  -jar "$paramFitsToolLoc" -s "$fitsOutputDir/$mergebasehash" -c "$fitsOutputDir/$githeadhash" -k "$githeadhash"
 	case "$?" in
 		# Test passed so no need to look for broken revision
 		0 )
 		echo "Test of HEAD against branch base succeeded, no broken revision to find."
 		resetHead   
 		exit 0;
 		;;
 		# Test of dev branch HEAD against master couldn't be performed
		# We probably don't want to go on until tests execute
 		125 )
 		echo "Test of HEAD against branch base could not be performed"
 		resetHead
 		exit 125;
 		;;
 		# Test failed, exit for now but the start for the
		# revision that broke the test starts here
 		* )
 		echo "Test of HEAD against branch base failed"
 		resetHead
 		return 1;
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
# Grab the SHA IDs of the current HEAD and the branch's merge BASE commits
getHeadHash
findMergeBaseHash;
echo "Compring HEAD $githeadhash against merge BASE $mergebasehash"
# Set reset HEAD flag, we're about to check out changes
resetHead=1
# Checkout master branch BASE revision
checkoutRevision "$mergebasehash"
# Build and execute branch BASE revision for comparison
buildAndExecuteFits

# Build current version of FITS
resetHead;
# Checkout current HEAD revision
checkoutRevision "$githeadhash"
# Build and execute current HEAD revision
buildAndExecuteFits

# Compare the results
testHeadAgainstMergeBase;
if [[ "$?" == "1" ]]
then
	echo "It's git-bisect time."
fi

# Reset repo to head
resetHead;

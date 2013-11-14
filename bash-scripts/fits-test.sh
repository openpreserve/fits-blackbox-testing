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
#   * Checks that there you're not on master.
#	* Builds the current branch and generates test output.
#   * Checks out the master commit that's the source of this branch.
#	* Builds FITS and generates the more test output.
#	* Use the FITS testing tool to compare the output.
#	* If successful, i.e. the output is unchanged report success.
#   * If unsuccessful use git-bisect to find the last good commit. 
##

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Globals to hold the checked param vals
paramFitsToolLoc=
paramCorporaLoc=

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

getCurrentBranchHash() {
	gitcurrenthash=$(git rev-parse HEAD 2>&1)
}

# Find the hash of the current branch's split from master
findMergeBaseHash() {
	gitshow=$(git show --pretty=format:"%H" `git merge-base "$currentBranch" master` 2>&1)
	shaRegEx="^[0-9a-f]{40}$"
	while IFS= read -r
	do 
		if [[ $REPLY =~ $shaRegEx ]]
		then
			mergeBaseHash=$REPLY
		fi
	done <<< "$gitshow"
}

checkGitStatus() {
	bash "$SCRIPT_DIR/check-git-status.sh"
	if (( $? != 0))
	then
		exit $?;
	fi
}

buildFits() {
	bash "$SCRIPT_DIR/fits-ant-release.sh"
	if (( $? != 0))
	then
		exit $?;
	fi
}

executeFits() {
	outputDir="./.output/$gitcurrenthash"
	mkdir -p "$outputDir"
	bash "$SCRIPT_DIR/execute-fits.sh" "$paramCorporaLoc" "$outputDir" 
	if (( $? != 0))
	then
		exit $?;
	fi
}

##
# Script Execution Starts HERE 
##
checkParams "$@";
checkGitStatus;
echo "In git repo ${PWD##*/}"
checkMaster;
echo "On branch: $currentBranch"
getCurrentBranchHash
echo "HEAD commit $gitcurrenthash"
findMergeBaseHash;
echo "master base commit $mergeBaseHash"
buildFits;
echo "Exectuting fits on corpora $paramCorporaLoc"
executeFits;

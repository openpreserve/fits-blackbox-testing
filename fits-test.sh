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

ANT_SUCCESSFUL="SUCCESSFUL"
# Globals to hold the checked param vals
paramFitsToolLoc=
paramCorporaLoc=
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
		echo "Output corpora directory not found: $paramCorporaLoc"
		exit 1;
	fi
}

# Check we're in a git repository, and there's no uncommitted changes 
checkGitStatus() {
	gitstatus=$(git status -z 2>&1)
	while IFS= read -r
	do
		# Check we're in a git repo
		isNotGitRepoFatal "$REPLY"
		
		# Other output means there's changes to commit
		if [[ ! -z "$REPLY" ]]
		then
			echo "There are uncommitted changes in this repo: $REPLY EXIT NEEDED HERE";
			# exit 1;
		fi
	done <<< "$gitstatus"
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

# Test param string for not a git repo output and exit if found
isNotGitRepoFatal() {
	notGitRegEx="^fatal: Not a git repository"
	if [[ $1 =~ $notGitRegEx ]]
	then
		echo "$1"
		exit 1;
	fi
}

buildFits() {
	cd ~/dev/git-hub/openplanets/fits
	antCommand=$(ant clean 2>&1)
	wasAntSuccessful "$antCommand"
	antStatus=$?
	if (( ! $antStatus == 1 ))
	then
		echo "ant status: $antSatus"
	fi
	 
	antCommand=$(ant compile 2>&1)
	wasAntSuccessful "$antCommand"
	if (( ! $antStatus == 1 ))
	then
		echo "ant status: $antSatus"
	fi
	
	ant release <<< "release"
}

wasAntSuccessful() {
	antCommand=$1
	buildRegEx="^BUILD ([A-Z]*)$"
	antStatus="UNKOWN"
	while IFS= read -r
	do
		if [[ $REPLY =~ $buildRegEx ]]
		then
			antStatus="${BASH_REMATCH[1]}"
		fi
	done <<< "$antCommand"

	if [[ $antStatus == $ANT_SUCCESSFUL ]]
	then 
		return 1;
	fi
	
	return 0;
}

checkParams "$@";
checkGitStatus;
echo "In git repo ${PWD##*/}"
checkMaster;
echo "Current branch: $currentBranch"
findMergeBaseHash;
buildFits;
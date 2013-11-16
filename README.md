#About fits-blackbox-testing

This project provides test scripts and tools for software developers contributing to the Harvard File Information Toolkit ([FITS](https://github.com/harvard-lts/fits)) project.

#Features and Roadmap

##Features

Version 0.0.1

- Tests current committted development branch HEAD against master BASE

##Roadmap:

- If candidate HEAD fails test use [git bisect](https://www.kernel.org/pub/software/scm/git/docs/git-bisect.html) to identify commit to BLAME.
- Add sample FITS testing tools.

#Installation and Use

## Pre-Requisites

Before installation you'll require:

- an environment capable of running bash scripts.
- Java (versions yet to be tested) to execute the testing tool and FITS.
- Maven to build the Java test tools.
- a working git installation.

The scripts have been developed and tested on a Debian 7 linux box.
The test scripts should run fine on OSX systems that meet the other pre-requisites.
Windows users may be able to run the scripts using [Cygwin](http://www.cygwin.com), but this hasn't been tested.

##Installation

Clone this repository:
```
git clone git@github.com:openplanets/fits-blackbox-testing.git
```
*TODO:* Add working install instructions once project structure updated.

## Use

The tools are meant to help automate two tasks:

- Developers wanting to test the current HEAD of their development branch again the base of the branch.
- Maintainers given a set of commits that now fail testing, wishing to identify the earliest failed commit.
 
These two workflows are described below.

###Testing Candidate HEAD Against the Branch Base

To use the tool you must be developing in a FITS git repository, working on any branch other than master, with no uncommitted changes.
You can test this by issuing the command:
```
git status
```
You should see:
```
# On branch <my branch name>
nothing to commit (working directory clean)
```
and typing ```git status -s``` returns no output.

*TODO:* Document this workflow with worked examples using FITS branch.

###Identifying the First Commit in Branch History to Fail Testing

This functionality to be implemented.

*TODO:* Implement git bisect run testing to find broken commit 

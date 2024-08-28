#!/usr/bin/perl -s
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2024 Doug Donohoe
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# For the full License text, please see the LICENSE.txt file
# in the root directory of this project.
# 
# The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
# graphics, text, and documentation found in this repository (including but not
# limited to written documentation, website content, and marketing materials) 
# are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
# 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
# without explicit written permission for any uses not covered by this License.
# For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
# in the root directory of this project.
# 
# For inquiries regarding commercial licensing of this source code or 
# the use of names, logos, images, text, or other assets, please contact 
# doug [at] donohoe [dot] info.
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
#
# Script to build some/all installers for release
#
# Files get copied into $RELEASEDIR, where this can be used to do a smoke test:
#
#  cd release
#  java -cp .:`ls *.jar | tr '\n' ':'` com.donohoedigital.games.poker.PokerMain
#
# options: see below
#
# TODO: code signing, get version from code to pass to install4j

$OSTYPE=$^O;
# mac?
if ($OSTYPE =~ "darwin.*")
{
	$MAC = 1;
	$OSNAME = "mac";
}
elsif ($OSTYPE =~ "linux.*")
{
    $LINUX = 1;
	$OSNAME = "linux";
}
else
{
	$WIN = 1;
	$OSNAME = "win";
}

#
# product we are building and location of builds
#
$FULL = "DD Poker 3";
$FULLPRODUCT="DD Poker 3.app";
$INSTALL="ddpoker3";
$INSTALLER_START="ddpoker";
$PRODUCT="poker";
$MAIN_MVN_MODULE="poker";
$MVN_VERSION="3.0"; # current version (should match poker maven module version)
$BUILDDIR="poker3.x";
$BASEDIR="builds/$BUILDDIR";

# base dir
if ($MAC)
{
	$BASELOC="$ENV{HOME}/$BASEDIR";
}
else
{
	$BASELOC="C:/$BASEDIR";
}

# dev option - special case
if ($dev)
{
	$BASELOC="$ENV{WORK}";
	$nogit=1;
}

# place to copy installers
$DESTDIR="$BASELOC/ddpoker/installer/builds";

#
# builds
#
if ($dev)
{
	build("dev");
}
elsif ($full)
{
	build("full");
}

# done
if (!$builtsomething && !$scp)
{
	print ("\noptions:\n"); 
	print ("   -full            build full installer in ~/$BASEDIR\n");
	print ("   -dev             build installer in local dev tree [-full ignored]\n");
	print ("   -clean           use clean flags with mvn and buildrelease\n");
	print ("   -nogit           skip git update step\n");
	print ("   -nomvn           skip mvn step\n");
	print ("   -nounpack        skip unpack jars step\n");
	print ("   -nobuildrelease  skip buildrelease step\n");
	print ("   -noinstaller     skip install4j build step\n");
	print ("   -scp             scp all installers to remote machine\n");
	print ("   -github          copy all installers to github\n");
	print ("   -exitearly       just print env vars\n");
	print ("\n");
	exit(1);
}
else
{
	# scp to remote machine TODO: github releases
	if ($scp)
	{
	    # create script to be executed by 'buildall'
		open(SCP, ">/tmp/buildall.scp") || die "Couldn't open /tmp/buildall.scp";
		print (SCP "cd $DESTDIR\n");
		print (SCP "echo Copying: *.*\n");
		print (SCP "scp *.* $ENV{AWS_APACHE}:/home/donohoe/junk/$PRODUCT\n");
		close(SCP);
	}

	exit(0);
}

#####
##### Sub routines
#####
sub build
{
	my($module) = @_;

	$indent = "    ";
	$builtsomething = 1;

	if ($module eq "dev")
	{
		$displayname = "work";
		$DEVPARENT = $BASELOC
	}
	else
	{
		$displayname = "$BASEDIR/$module";
		$DEVPARENT = "$BASELOC/$module";
	}
	$DEVDIR = "$DEVPARENT/ddpoker";
	$STAGINGDIR = "$DEVDIR/build";
	$MVNDIR = "$DEVDIR/code";
	$INSDIR = "$DEVDIR/installer/install4j";
    $RELEASEDIR="$DEVDIR/release";

	print ("\nBUILDING $module ($displayname)\n\n");

	# delimiter
    if ($WIN) {
        $DELIM = ";";
    } else {
        $DELIM = ":";
    }

	# make sure dev parent dir exists
	`mkdir -vp $DEVPARENT`;

	print("  OSTYPE:      $OSTYPE\n");
	print("  OSNAME:      $OSNAME\n");
	print("  MVN_VERSION: $MVN_VERSION\n");
	print("  BASELOC:     $BASELOC\n");
	print("  BASEDIR:     $BASEDIR\n");
	print("  DEVPARENT:   $DEVPARENT\n");
	print("  DEVDIR:      $DEVDIR\n");
	print("  MVNDIR:      $MVNDIR\n");
	print("  STAGINGDIR:  $STAGINGDIR\n");
	print("  RELEASEDIR:  $RELEASEDIR\n");
	print("  INSDIR:      $INSDIR\n");
	print("  $DESTDIR:    $DESTDIR\n");
	print("\n");

	exit(0) if ($exitearly);

	# git update
	if (!$nogit)
	{
		cd($DEVPARENT);

		if ( ! -d $DEVDIR ) {
            runIndented("git clone --progress --no-checkout git\@github.com:dougdonohoe/ddpoker.git", $indent);
            cd($DEVDIR);
            runIndented("git checkout --progress", $indent); # TODO git checkout <tag>
		} else {
		    cd($DEVDIR);
            runIndented("git pull --progress ", $indent);
		}
	} else {
	    print($indent . "Skipping git.\n");
	}

    # make sure various dir exist (need to do this after git otherwise clone will fail)
	`mkdir -vp $RELEASEDIR`;
	`mkdir -vp $STAGINGDIR`;
    `mkdir -vp $DESTDIR`;

	# compile
	if (!$nomvn)
	{
		cd($MVNDIR);

		if ($clean)
		{
			runIndented("mvn clean", $indent);
		}
		runIndented("mvn install -Dmaven.test.skip=true", $indent);
	}

	# get version
	$VERSION=`runjava pokerengine com.donohoedigital.games.poker.engine.PrintVersion`;
	chop $VERSION;
	$VERSION_FILE = $VERSION =~ s/\./_/gr;
	print("\nVERSION is '$VERSION', file extension is '$VERSION_FILE'\n");

	# unpack jars and generate config files
	if (!$nounpack)
	{
		# determine git hash version - TODO: use a tag instead?
		cd("$DEVPARENT/ddpoker/code");
		$githash = `git rev-parse --short HEAD`;
		chop $githash;

		# go to staging dir
		cd($STAGINGDIR);

		# clean
		if ($clean)
		{
			runIndented("rm -rvf $STAGINGDIR/*", $indent);
		}

		# copy/unpack jars
		$targetdir = "$MVNDIR/$MAIN_MVN_MODULE/target";
		$classpathfile = "$targetdir/classpath.txt";
		open (CP, "$classpathfile") || die "Couldn't open $classspathfile";
		while ($classpath = <CP>)
		{
			# get dependencies
			@jars = split($DELIM, $classpath);

			# add main jar (not in classpath.txt)
			push @jars, "$targetdir/$MAIN_MVN_MODULE-$MVN_VERSION.jar";

			# loop and extract Donohoe Digital jars, copy rest
			for $jar (@jars) {
				$jar =~ s/\\/\//g; # convert \ to /
				if ($jar =~ /\/target\//) {
					runIndented("jar xvf $jar", $indent);
				} else {
					runIndented("cp $jar .", $indent);
				}
			}
		}
		close(CP);

		# copy installer jar
		$jar = "$MVNDIR/installer/target/installer-$MVN_VERSION.jar";
		runIndented("cp $jar .", $indent);

		# remove META-INF from jars
		runIndented("rm -rvf META-INF", $indent);

		# write git build number
		`echo "build.number= $githash" > config/buildnumber.properties`;
	}

	# buildrelease
	if (!$nobuildrelease)
	{
		cd($DEVDIR);
		$OPTIONS="-releasedir $RELEASEDIR -devdir $DEVDIR -stagingdir $STAGINGDIR -product $PRODUCT";
		if ($module eq "testing") {
			$OPTIONS .= " -testing";
		}
		if ($clean) {
			$OPTIONS .= " -clean";
		}

		runIndented("buildrelease.pl $OPTIONS", $indent);
	}

	# run installer
	if (!$noinstaller)
	{
		cd($INSDIR);

		# determine installer file name
		$install4j = $PRODUCT . ".install4j";

		# copy cert
        runIndented("~/work/donohoe/ddpoker/cp-certs.sh '$DEVDIR/target'");

        # get password
        $mac_pw = `~/work/donohoe/ddpoker/get-password.sh "ddpoker-dev-app-p12" "Apple P12 cert"`;
        chop $mac_pw;

		# run install4j
		#   TODO: --win-keystore-password    Set private key password for Windows code signing.
        $cmd = "/Applications/install4j.app/Contents/Resources/app/bin/install4jc --release=$VERSION --mac-keystore-password='$mac_pw' --build-selected $install4j\n";
        runIndented($cmd);

        # Set icons in Mac installer and notarize
        runIndented("${DEVDIR}/tools/bin/mac-set-icons-notarize.sh $VERSION_FILE");

        print("Installers are in $DESTDIR\n")
	}

	# push installers to GitHub
	if ($github)
	{
	    # TODO: hashes in notes?
	    $notes="DD Poker release $VERSION";
        my $files = join " ", map { "$DESTDIR/ddpoker$VERSION_FILE$_" } (".dmg", ".exe", ".sh");
	    runIndented("gh release create $VERSION --title 'DD Poker $VERSION' --draft --notes '$notes' $files");
	}
}

# run given command and indent output, and optionally filter lines
sub runIndented
{
	my($command, $indent, $filter) = @_;
    print("\n" . $indent . "Running '$command'...\n");
	open (OUTPUT, "$command 2>&1 |") || die "can't open $!";
	while (<OUTPUT>)
	{
		next if ($filter && $_ =~ /$filter/);
		print($indent . "  " . $_);
	}
	close OUTPUT || die "\n*** Error code ($?) running '$command' $!\n\n";
}	

# chdir with error checking
sub cd
{
	my($dir) = @_;

	chdir($dir) || die "Can't chdir to $dir";
}

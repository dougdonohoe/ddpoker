#!/usr/bin/perl -s
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2026 Doug Donohoe
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

use Cwd;

# where the script was invoked from, captured before anything cd()s away - see
# pullOriginalDir()
$ORIGDIR = getcwd();

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
$GITREPO="ddpoker";

# platform token -> installer extension, README label
# (tokens must match mediaFileName in poker.install4j)
@PLATFORMS = ( ["mac", "dmg", "Mac"], ["windows", "exe", "Windows"], ["linux", "sh", "Linux"] );

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

#
# -github-dryrun is a rehearsal of -github.  'perl -s' can't put a hyphenated switch
# in a normal variable, so pick it up via a symbolic ref.
#
$githubdryrun = ${'github-dryrun'};
$github = 1 if ($githubdryrun);

#
# if github, skip git, mvn, unpack, buildrelease, installer (assumes done previously).
# The idea is that you first build (and verify) then upload to github
#
if ($github) {
 $nogit = 1;
 $nomvn = 1;
 $nobuildrelease = 1;
 $nounpack = 1;
 $noinstaller = 1;
}

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
if (!$builtsomething)
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
	print ("   -nonotarize      skip notarize step\n");
	print ("   -github          create a release at github with all installers\n");
	print ("   -github-dryrun   rehearse -github: draft notes, echo the gh command, change nothing\n");
	print ("   -exitearly       just print env vars\n");
	print ("\n");
	exit(1);
}
else
{
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
    $RELEASEDIR="$DEVDIR/release";
	$INS4JDIR = "$DEVDIR/installer/install4j";
	$INSTALLERDIR="$DEVDIR/installer/builds";

	print ("\nBUILDING $module ($displayname)\n\n");

	# delimiter
    if ($WIN) {
        $DELIM = ";";
    } else {
        $DELIM = ":";
    }

	# make sure dev parent dir exists
	`mkdir -vp $DEVPARENT`;

	print("  OSTYPE:       $OSTYPE\n");
	print("  OSNAME:       $OSNAME\n");
	print("  MVN_VERSION:  $MVN_VERSION\n");
	print("  BASELOC:      $BASELOC\n");
	print("  BASEDIR:      $BASEDIR\n");
	print("  DEVPARENT:    $DEVPARENT\n");
	print("  DEVDIR:       $DEVDIR\n");
	print("  MVNDIR:       $MVNDIR\n");
	print("  STAGINGDIR:   $STAGINGDIR\n");
	print("  RELEASEDIR:   $RELEASEDIR\n");
	print("  INS4JDIR:     $INS4JDIR\n");
	print("  INSTALLERDIR: $INSTALLERDIR\n");
	print("\n");

	exit(0) if ($exitearly);

	# git update
	if (!$nogit)
	{
		cd($DEVPARENT);

		if ( ! -d $DEVDIR ) {
            runIndented("git clone --progress --no-checkout git\@github.com:dougdonohoe/$GITREPO.git", $indent);
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
    `mkdir -vp $INSTALLERDIR`;

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
	getVersion();

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
		cd($INS4JDIR);

		# determine installer file name
		$install4j = $PRODUCT . ".install4j";

		# copy cert
        runIndented("~/work/donohoe/installer/cp-certs.sh '$DEVDIR/target'");

        # get passwords
        $mac_pw = `~/work/donohoe/installer/get-password.sh "ddpoker-dev-app-p12" "Apple P12 cert"`;
        chop $mac_pw;
        $win_pw = `~/work/donohoe/installer/get-password.sh "ddpoker-sectigo-token-password" "Windows Sectigo cert"`;
        chop $win_pw;

        # clean old builds
        runIndented("rm -vf $INSTALLERDIR/${INSTALLER_START}_*_${VERSION_FILE}.*");

		# run install4j
        $cmd = "/Applications/install4j.app/Contents/Resources/app/bin/install4jc --release=$VERSION " .
                "--mac-keystore-password='$mac_pw' --win-keystore-password='$win_pw' --build-selected $install4j";
        runIndented($cmd);

        # Set icons in Mac installer and notarize
        if (!$nonotarize) {
            runIndented("${DEVDIR}/tools/bin/mac-set-icons-notarize.sh " . installerName("mac", "dmg"));
        } else {
            print($indent . "Notarization skipped.\n");
        }

        # Create md5sums.txt
        cd($INSTALLERDIR);
        runIndented("md5 " . installerNames() . " > md5sums.txt");

        print("Installers are in $INSTALLERDIR\n");
	}

	# push installers to GitHub
	if ($github)
	{
		# -github skips the git step, so make sure we can still push the README
		# update before we publish anything
		checkNotBehind();
		checkInstallers();

		# draft release notes from whatsnew.html so they can be checked over.  These
		# are kept alongside the installers, one file per version.
		$notes = releaseNotes($VERSION);
		$notesfile = releaseNotesName();
		writeFile("$INSTALLERDIR/$notesfile", $notes);

		print("\nDraft release notes for $VERSION ($INSTALLERDIR/$notesfile):\n\n");
		print(("-" x 78) . "\n");
		print($notes);
		print(("-" x 78) . "\n");

		# check the notes over before going any further.  The dry run asks too, so
		# the whole flow gets rehearsed.
		confirm($githubdryrun
		        ? "Continue the dry run with these notes?"
		        : "Create the GitHub release for $VERSION with these notes?");

		cd($INSTALLERDIR);
		$cmd = "gh release create $VERSION --title 'DD Poker $VERSION' " .
		       "--notes-file $notesfile " . installerNames();

		if ($githubdryrun)
		{
			# check the README markers now, since we stop before rewriting them
			updateReadme($VERSION, 1);

			# report the same pull decision the real run would make, skip reason and all
			my($pullok, $pulldetail) = pullPlan();

			print("\nDRY RUN - would run:\n\n    $cmd\n");
			print("\nDRY RUN - would then point the README.md installer links at $VERSION,\n");
			print("show the diff, and commit/push it.  It would then " .
			      ($pullok ? "run 'git pull --tags' in $pulldetail"
			               : "skip the git pull ($pulldetail)") . ".\n");
			print("No release was created and README.md was not touched\n");
			print("(only $INSTALLERDIR/$notesfile was written).\n\n");
			exit(0);
		}

		runIndented($cmd, $indent);

		# post-release: point the README at the installers we just published
		updateReadme($VERSION, 0);

		cd($DEVDIR);
		runIndented("git diff README.md", $indent);
		confirm("Commit and push this README.md change?");
		runIndented("git add README.md", $indent);
		runIndented("git commit -m 'Update README installer links for $VERSION'", $indent);
		runIndented("git push", $indent);

		# everything above happened in the build clone, so bring the tag and the README
		# commit back to wherever this was run from
		pullOriginalDir();
	}
}

# run given command and indent output, and optionally filter lines.  A non-zero exit
# normally aborts the build; $warnonly downgrades that to a warning, for steps that run
# after the release has already been published.
sub runIndented
{
	my($command, $indent, $filter, $warnonly) = @_;
    print("\n" . $indent . "Running '$command'...\n");
	open (OUTPUT, "$command 2>&1 |") || die "can't open $!";
	while (<OUTPUT>)
	{
		next if ($filter && $_ =~ /$filter/);
		print($indent . "  " . $_);
	}
	close OUTPUT;
    my $exit_code = $? >> 8;
    if ($exit_code != 0) {
        if ($warnonly) {
            print("\n*** Warning (exit code $exit_code) running '$command'\n\n");
        } else {
            print("\n*** Error (exit code $exit_code) running '$command'\n\n");
            exit($exit_code);
        }
    }
    return $exit_code;
}

# set VERSION and VERSION_FILE (assumes mvn has been run)
sub getVersion
{
    return if ($VERSION);
    # Set WORK to location of code so 'runjava' runs that code and not local code
	$VERSION=`WORK=$DEVPARENT runjava pokerengine com.donohoedigital.games.poker.engine.PrintVersion`;
	chop $VERSION;
	$VERSION_FILE = $VERSION =~ s/\./_/gr;
	print("\nVERSION is '$VERSION', file extension is '$VERSION_FILE'\n");
}

# installer file name for a platform, e.g. ddpoker_mac_3_1_6.dmg
sub installerName
{
	my($platform, $ext) = @_;

	return "${INSTALLER_START}_${platform}_${VERSION_FILE}.$ext";
}

# all installer file names, space separated
sub installerNames
{
	return join " ", map { installerName($$_[0], $$_[1]) } @PLATFORMS;
}

# release notes file name, e.g. release_notes_3_1_6.md
sub releaseNotesName
{
	return "release_notes_${VERSION_FILE}.md";
}

# read and return the entire contents of a file
sub slurp
{
	my($file) = @_;

	open (IN, "$file") || die "Couldn't open $file";
	local $/;
	my $contents = <IN>;
	close(IN);

	return $contents;
}

# write contents to a file
sub writeFile
{
	my($file, $contents) = @_;

	open (OUT, ">$file") || die "Couldn't write $file";
	print OUT $contents;
	close(OUT);
}

# ask a yes/no question, exiting unless the answer is yes
sub confirm
{
	my($prompt) = @_;

	print("\n$prompt [y/N] ");
	my $answer = <STDIN>;
	if ($answer !~ /^\s*y(es)?\s*$/i)
	{
		print("\nAborted.\n\n");
		exit(1);
	}
}

# -github skips the git step, so make sure the build clone is current.  Otherwise we
# could publish a release and then be unable to push the README update, and the
# installers we are about to publish would have been built from stale code.
sub checkNotBehind
{
	cd($DEVDIR);
	runIndented("git fetch origin", $indent);

	my $behind = `git rev-list --count HEAD..origin/main`;
	chop $behind;
	if ($behind > 0)
	{
		print("\n*** $DEVDIR is $behind commit(s) behind origin/main.\n");
		print("*** Re-run 'buildall.pl -full' so the installers match origin/main.\n\n");
		exit(1);
	}
}

# top of the git work tree containing $dir, or empty when $dir isn't in one
sub gitTopLevel
{
	my($dir) = @_;

	my $top = `git -C '$dir' rev-parse --show-toplevel 2>/dev/null`;
	chop $top;

	return $top;
}

# current branch name of the repo containing $dir ("HEAD" when detached)
sub gitBranch
{
	my($dir) = @_;

	my $branch = `git -C '$dir' rev-parse --abbrev-ref HEAD 2>/dev/null`;
	chop $branch;

	return $branch;
}

# Whether pullOriginalDir() should pull, as ($ok, $detail): the work tree to pull when
# $ok, otherwise the reason it is being skipped.  Split out from the pull itself so
# -github-dryrun can rehearse the same decision without touching anything.
sub pullPlan
{
	my $top = gitTopLevel($ORIGDIR);

	return (0, "$ORIGDIR is not a git repository") if (!$top);

	# with -dev (or if run from the build clone itself) the release was made right here,
	# so there is nothing to pull back
	return (0, "it ran from the build clone $top") if ($top eq gitTopLevel($DEVDIR));

	my $branch = gitBranch($ORIGDIR);
	return (0, "$top is on '$branch', not main") if ($branch ne "main");

	return (1, $top);
}

# -github does its git work in the build clone, so the dev tree this was launched from
# never sees the new tag or the README commit.  Pull them over, when that is safe to do.
sub pullOriginalDir
{
	my($ok, $detail) = pullPlan();

	if (!$ok)
	{
		print("\n" . $indent . "Skipping git pull: $detail.\n");
		return;
	}

	cd($ORIGDIR);

	# only a warning if this fails: the release is published and the README is pushed, so
	# a dirty or diverged dev tree shouldn't make a good release look like a broken build
	runIndented("git pull --tags --progress", $indent, undef, 1);
}

# build markdown release notes for $version from the whatsnew.html entry of the same
# version
sub releaseNotes
{
	my($version) = @_;

	my $whatsnew = "$DEVDIR/code/$MAIN_MVN_MODULE/src/main/resources/config/$PRODUCT/help/whatsnew.html";
	my $html = slurp($whatsnew);

	# the version header, then the <ul> of items that follows it
	$html =~ m{Version\s+\Q$version\E\s+-\s+([^<]+?)\s*</span>.*?<ul>(.*?)</ul>}s
		|| die "No 'Version $version' entry found in $whatsnew";
	my($date, $items) = ($1, $2);

	my $notes = "## Version $version - $date\n\n";
	while ($items =~ m{<li>(.*?)</li>}gs)
	{
		my $item = $1;
		$item =~ s{<tt>(.*?)</tt>}{`$1`}gs;    # fixed width -> code
		$item =~ s{<b>(.*?)</b>}{**$1**}gs;    # bold -> bold
		$item =~ s/&lt;/</g;
		$item =~ s/&gt;/>/g;
		$item =~ s/&amp;/&/g;
		$item =~ s/\s+/ /g;                    # undo the source line wrapping
		$item =~ s/^\s+|\s+$//g;
		$notes .= "- $item\n";
	}

	# changelog against the most recent release that isn't this one (so re-running
	# -github after the release exists still produces the same notes)
	my $prevcmd = "gh release list --limit 5 --json tagName " .
	              "--jq '[.[].tagName] | map(select(. != \"$version\")) | .[0] // \"\"'";
	my $prev = `$prevcmd`;
	chop $prev;
	if ($prev && $prev ne $version)
	{
		$notes .= "\n**Full Changelog**: " .
		          "https://github.com/dougdonohoe/$GITREPO/compare/$prev...$version\n";
	}

	# md5sums.txt is written by the installer step of the preceding -full run, so make
	# sure it is for this version and not left over from an earlier one
	my $verfile = $version =~ s/\./_/gr;
	my $md5file = "$INSTALLERDIR/md5sums.txt";
	die "$md5file not found - run 'buildall.pl -full' first" if (! -f $md5file);

	# match the "_<version>." out of the installer names rather than the bare version:
	# a plain "3_1" is also a substring of a left-over "3_1_6" build
	my $md5 = slurp($md5file);
	die "$md5file has no $verfile entries (stale?) - re-run 'buildall.pl -full'"
		if ($md5 !~ /_\Q$verfile\E\./);
	$notes .= "\n" . $md5;

	return $notes;
}

# make sure every installer we are about to upload actually exists
sub checkInstallers
{
	for my $platform (@PLATFORMS)
	{
		my $file = "$INSTALLERDIR/" . installerName($$platform[0], $$platform[1]);
		die "$file not found - run 'buildall.pl -full' first" if (! -f $file);
	}
}

# rewrite the installer links between the markers in README.md.  If $dryrun, only
# check that the markers are still there.
sub updateReadme
{
	my($version, $dryrun) = @_;

	my $readme = "$DEVDIR/README.md";
	my $begin = "<!-- installers:begin (updated by tools/bin/buildall.pl -github) -->";
	my $end = "<!-- installers:end -->";

	my $block = "$begin\nDownload the latest release, **$version**:\n\n";
	for my $platform (@PLATFORMS)
	{
		my($plat, $ext, $label) = @$platform;
		my $file = installerName($plat, $ext);
		$block .= "- **$label**: [$file]" .
		          "(https://github.com/dougdonohoe/$GITREPO/releases/download/$version/$file)\n";
	}
	$block .= $end;

	my $text = slurp($readme);
	($text =~ s/\Q$begin\E.*?\Q$end\E/$block/s)
		|| die "Could not find the installers:begin/end markers in $readme";

	writeFile($readme, $text) if (!$dryrun);
}

# chdir with error checking
sub cd
{
	my($dir) = @_;

	chdir($dir) || die "Can't chdir to $dir";
}

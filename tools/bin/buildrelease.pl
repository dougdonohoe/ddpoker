#!/usr/bin/perl 
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
# Script to recursively do CVS adds
#
# option: -clean clean dest dir first
# Note: options stripped from @ARGV by "perl -s" flag !!!

###
### MODULES
###

use Getopt::Long;	# command line option parsing

###
### COMMAND LINE OPTIONS
###

GetOptions("releasedir=s" => \$RELEASEDIR,	# Release directory
           "devdir=s" => \$DEVDIR,		    # Development directory
           "stagingdir=s" => \$STAGINGDIR,	# Build directory
           "product=s" => \$product,		# Product name
           "clean" => \$clean,				# Clean release directory
           "testing" => \$testing);			# Enable testing (keeps debug settings)

####
#### SETUP VARIABLES
####

# UNIX file separator - xplatform way of using this?
$SEP = "/"; 

# product
$poker = ($product eq "poker"); # backward compatibility with old options
if ($poker)
{
	$PRODUCT = "poker";
	$INSTALL = "ddpoker";
}
else
{
    print "Unknown product $PRODUCT\n";
    exit(1);
}

# TESTING
#
if ($testing)
{
	print "TESTING MODE ON\n";
}

print "\n";
print "DEVDIR is     $DEVDIR\n";
print "STAGINGDIR is $STAGINGDIR\n";
print "RELEASEDIR is $RELEASEDIR\n";
print "\n";

# clean
#
if ($clean)
{
	print "CLEANING $RELEASEDIR\n";
	print `rm -vrf "$RELEASEDIR"/*`;
}

# Copy class files, jars, resources
#
chdir ($STAGINGDIR);
processdir(".", ".");

# copy license files
#
chdir ("$DEVDIR/docs");
processdir(".", "license");

# TODO: copy root LICENSE.txt and LICENSE-CREATIVE-COMMONS.txt?

####
#### FUNCTIONS
####
#
# Process a directory by looping through each,
# looking for jar files and subdirs
#
sub processdir
{
	my($parent, $dir) = @_;
	my($fullpath) = $parent . $SEP . $dir;

    # tweak paths so debug output doesn't have "./" or "foo/./bar"
	if ($parent eq "." && $dir eq ".") {
        $fullpath = "";
	} elsif ($parent eq ".") {
	    $fullpath = $dir;
	}
	my($DIRHANDLE) = "DIR" . $parent . $dir ;  # unique DIR handle for this dir
	my($fullentry) = "";

	my($DEST) = $RELEASEDIR;
	if ($fullpath ne "") {
        $DEST .= $SEP . $fullpath;
	}

	if ( ! -d $DEST )
	{
		`mkdir -p \"$DEST\"`;
	}

	print "Processing: $fullpath\n";

	#
	# Open dir and loop through each entry
	#
	opendir($DIRHANDLE, $fullpath || ".") || error("Failed to open $fullpath ($!)");
	my @files = sort readdir($DIRHANDLE);
	closedir($DIRHANDLE);

	foreach my $entry (@files) {
        if ($fullpath eq "") {
            $fullentry = $entry;
            $innerfullpath = ".";
        } else {
    		$fullentry = $fullpath . $SEP . $entry;
            $innerfullpath = $fullpath;
        }
		$fulldest = $DEST . $SEP . $entry;

		#
		# Skip . .. .dotfiles overrides and non-client log4j files
		#
		next if ($entry =~ /^\./);
		next if ($entry =~ "override");
		next if ($entry =~ "log4j.server");
		next if ($entry =~ "log4j.webapp");
		next if ($entry =~ "log4j.cmdline");

		#
		# If this is a directory, recurse
		#
		if ( -d $fullentry ) {
			processdir($innerfullpath, $entry);
		} 
		#
		# Else if common.properties file, remove debug stmts
		#
		elsif ($entry =~ /common.properties/ && !$testing)
		{
			docopyskip($fullentry, $fulldest, "settings.debug");
		}
		#
		# else just copy file
		#
		else 
		{
			docopy($fullentry, $fulldest);
		}
	}
}

sub docopy {
	my($from, $to) = @_;
	
	$timefrom = modtime($from);
	$timeto = modtime($to);
	if ($timefrom > $timeto)
	{
		$from =~ s/\$/\\\$/g;
		$to =~ s/\$/\\\$/g;
		print "Copying $from to $to\n";
		`cp "$from" "$to"`;
	}
}

sub docopyskip {
	my($from, $to, $skip) = @_;
	$timefrom = modtime($from);
	$timeto = modtime($to);
	if ($timefrom > $timeto)
	{
		print "Copy/Skip $from to $to (removing lines with '$skip')\n";
		open (CHANGE, "$from");
		open (CHANGED, ">$to");

		while ($line = <CHANGE>)
		{
			next if ($line =~ /$skip/);
			print CHANGED $line;
		}
		close CHANGE;
		close CHANGED;
	}
}

sub modtime {
	my($file) = @_;
	($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
	 $atime,$mtime,$ctime,$blksize,$blocks) = stat $file;

	#print "Starting:  $file-$mtime\n";
	return $mtime;	
}

#
# Handle errors
#
sub error {
	my($msg) = @_;
	die "Error: $msg\n";
}

#!/usr/bin/perl
#########################################################################
#
# Check ResourceBundle key-value pairs consistency with java source files.
#                   
#
# Copyright (C) 2021 Jerome Lelasseux jl@jjazzlab.org
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
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#
#########################################################################

use warnings;
use strict;
use File::Basename;
use Getopt::Long qw(GetOptions);
use File::Find qw(find);
use File::Temp qw(tempfile);
#use File::Spec qw(abs2rel);       # not needed anymore in perl 5.40 (got a warning)
use Cwd qw();

binmode(STDOUT, ":utf8");          # treat as if it is UTF-8
binmode(STDIN, ":encoding(utf8)"); # actually check if it is UTF-8



# These are special JJazzLab keys without any reference in the java code, but which should not be removed by --remove-extra
my %doNotRemoveKeys;
# Base module (predefined Netbeans properties)
$doNotRemoveKeys{"Services/AutoupdateType/org_jjazz_base_update_center.instance"} = 1;
$doNotRemoveKeys{"org_jjazz_base_update_center"} = 1;
# MixConsole module (reference from layer.xml)
$doNotRemoveKeys{"MixConsoleMenuBarEdit"} = 1;
$doNotRemoveKeys{"MixConsoleMenuBarFile"} = 1;
# PhraseTransform module (key names are programmatically built in ResUtil.getString())
$doNotRemoveKeys{"AddCongas1_name"} = 1;
$doNotRemoveKeys{"AddCongas1_desc"} = 1;
$doNotRemoveKeys{"AddCongas2_name"} = 1;
$doNotRemoveKeys{"AddCongas2_desc"} = 1;
$doNotRemoveKeys{"AddCongas3_name"} = 1;
$doNotRemoveKeys{"AddCongas3_desc"} = 1;
$doNotRemoveKeys{"AddCongas4_name"} = 1;
$doNotRemoveKeys{"AddCongas4_desc"} = 1;



sub usage
{
	print "\nUSAGE: $0 [options] maven_project_dir1 [maven_project_dir2] ... \n";
	print "Parse all java files in Maven projet dir(s) to find used ResourceBundle keys, then check for extra or missing keys in bundle properties files.\n";
	print "Also checks if there are java U-based hexadecimal escapes (a.k.a. unicode escapes) like \\u00E1 into the properties files, or if file is empty.\n";
    print "PARAMETERS:\n";
	print "  maven_project_dir: a maven standard project directory which must contain a pom.xml file at its root.\n";
	print "OPTIONS: \n";
	print "  --remove-extra : remove extra keys from bundle properties files.\n";	
	print "  --remove-emptyfiles : remove empty bundle properties files.\n";	
	print "  --missing-only : only show missing keys.\n";	
	print "  --ignore-translations : only process 'Bundle.properties files', ignoring translation variants 'Bundle_*.properties' files.\n";
	print "  --ignore-keys=key1,key2,... : one or more keys to be ignored\n";
	print "AUTHOR: Jerome Lelasseux \@2024\n";	
	exit;
}

# Command line arguments
my $removeExtra=0;
my $removeEmptyFiles=0;
my $ignoreTranslations=0;
my $missingOnly=0;
my $ignoredKeys=0;
GetOptions(
	'remove-extra' => \$removeExtra,
	'remove-emptyfiles' => \$removeEmptyFiles,	
	'missing-only' => \$missingOnly,	
	'ignore-translations' => \$ignoreTranslations,    
	'ignore-keys=s' => \$ignoredKeys	
) or usage();
usage() if ($#ARGV < 0);
if ($ignoredKeys)
{
	my @keys = split(',', $ignoredKeys);
	foreach my $key (@keys)
	{					
		$doNotRemoveKeys{$key} = 1;
	}
}
		



foreach my $dir (@ARGV)
{
	if (-d $dir)
	{
		processProjectDir($dir);
	}
}



#================================================================
# subroutines
#================================================================


# $1 = a maven project dir
sub processProjectDir
{
	my ($projectDir) = @_;
    my $srcDir="$projectDir/src/main/java";
	my $resDir="$projectDir/src/main/resources";
	
	print "== ============================================================\n";
	print "PROCESSING $projectDir... ";
	
	if (! -d $projectDir || ! -f "$projectDir/pom.xml" || ! -d "$projectDir/src/main/java")
	{
		print "skipped - invalid Maven project dir\n";
		return;
	}

	if (! -d "$projectDir/src/main/resources")
	{
		print "skipped - no resources dir\n";	
		return;
	}
	print "\n";
	
	
	
	# Associate for each relative-dir a hash reference containing the bundle keys for java files in that relative dir
	my %relDirKeys;		
	find ( sub {
			return unless -f;       
			return unless /\.java$/;  
			my $relPath=File::Spec->abs2rel($File::Find::dir, $srcDir);
			if (! $relDirKeys{$relPath})
				{
					# print "Creating empty hash for $relPath\n";
					$relDirKeys{$relPath} = {};   
				}
			my %javaKeys=processJavaFile($_);		
			addToHash($relDirKeys{$relPath}, \%javaKeys);			
			}, 
		$srcDir );
			
	
	#printHash("relDirKeys", \%relDirKeys);
	
	
	# Now scan the properties files to check contents
	find ( sub {
				return unless -f;       
				return unless /^Bundle.*properties$/;  
				return if /Bundle_/ && $ignoreTranslations;
				my $fn = $_;
				my $relPath=File::Spec->abs2rel($File::Find::dir, $resDir);						
				my $relFn="$relPath/$fn";
				my $javaKeysHRef=$relDirKeys{$relPath};		
				my %keyValuePairs=processBundleFile($_);		
				
				
				# check consistency
				if (! $missingOnly)
				{				
				
					if ($removeExtra)
					{
						# Rewrite file with extra keys removed
						open my $handle, ">", "$fn" or die "Can't create $fn\n";
						while (my($key, $value) = each %keyValuePairs ) 
						{
							if ($javaKeysHRef->{$key} || $doNotRemoveKeys{$key})
							{
								print $handle "$key=$value\n";
							} else
							{
								printf("Extra: %-40s    (%s)  REMOVED\n", $key, $relFn);
							}
						}
						close $handle;	
					}
					else
					{
						# Just display the extra keys
						 while (my($key, $value) = each %keyValuePairs ) 
						 {
							if (!$javaKeysHRef->{$key} && !$doNotRemoveKeys{$key})
							{
								printf("Extra: %-40s    (%s) \n", $key, $relFn);
							}
						 }
					}
				}
				 
				 while (my($key, $value) = each %$javaKeysHRef ) 
				 {
					if (!$keyValuePairs{$key})
					{
						printf("Missing: %-40s    (%s) \n", $key, $relFn);
					}
				} 	
				
				if (! %keyValuePairs)
				{
					print "###### WARNING empty file : $relFn";
					if ($removeEmptyFiles)
					{
						print "     REMOVED\n";
						unlink($fn);																
					}
					else
					{
						print "\n";
					}					
				}				
				
			}, 
		$resDir );	
	
}


# Analyze one line of java source code to find used ResourceBundle key(s).
# NOTE: This should be customized to match how you use ResourceBundle strings in your program.
# $1 line of code
# returns: 0, 1 or more ResourceBundle keys
sub getKeysFromJavaLine
{
    my ($line) = @_;
	if ($line =~ /^\s*\/\//)
	{
		return;
	}
	if ($line =~ /[^.]ResUtil\.getString\([^"]*$/)
	{
		print "###### WARNING unparsable key string within ResUtil.getString(...) detected, ignored : $line\n";
		return;
	}
	my @keys1 = ($line =~ /ResUtil\.getString\([^"]*"([^"]+)/g);    # g modifier power! see https://perldoc.perl.org/perlretut#Global-matching		
	my @keys2 = ($line =~ /NbBundle\.getMessage\([^"]*"([^"]+)/g);
	my @keys3 = ($line =~ /NbBundle\.getBundle\(.*class\)\.getString\([^"]*"([^"]+)/g);
	my @keys4 = ($line =~ /displayName\s*=\s*"#([^"]+)/g);
	my @keys5 = ($line =~ /categoryName\s*=\s*"#([^"]+)/g);
	my @keys6 = ($line =~ /keywords\s*=\s*"#([^"]+)/g);
    return (@keys1, @keys2, @keys3, @keys4, @keys5, @keys6);
}


# $1 java file name
# returns: a hash of the ResourceBundle keys found in the java file 
sub processJavaFile
{
	my ($javaFile) = @_;
	my %res;	
	# print "processJavaFile() $javaFile\n";
	open my $handle, '<', $javaFile or die "Cannot open $javaFile for reading: $!";	
	while (<$handle>)
	{
		chomp;	
		my @keys = getKeysFromJavaLine($_);
		for my $key (@keys)
		{
			$res{$key} = 1;
		}					
	}
	close $handle;	
	
	return %res;
}

# $1 ResourceBundle file name
# returns: hash with all key-value pairs
sub processBundleFile
{
    my ($file) = @_;
    my %res;
	if (-f $file)
	{
		open my $handle, '<', $file or die "Cannot open $file for reading: $!";	
		while (<$handle>)
		{
				chomp;	
				next if /^\s*#/ || ! /=/;     
				
				print "###### WARNING Possible Java U-based unicode escapes found (Ã might be legit in portugese) : $_\n" if (/u00|Ã/);					
				my $eqIndex = index($_, "=");
				my $key = substr($_, 0, $eqIndex);
				my $val = substr($_, $eqIndex+1);
				$res{$key} = $val;
		}
		close $handle;	
	}	
    return %res;	
}


# $1 a hash ref
# $2 another hash ref
# Add $2 data into $1
sub addToHash
{
	my ($h1ref, $h2ref) = @_;
    while (my($key, $value) = each %$h2ref) 
    {
        $h1ref->{ $key } = $value;
    }
}


# $1 hash variable name
# $2 hash ref
sub printHash
{    
	my ($n, $href) = @_;
    print "$n=\n";
    while (my($key, $value) = each %$href ) 
    {
		print "  $key: ";
		if (ref $value eq ref {}) 
		{
			# Special case, value is itself an hash ref
			print "\n";
			 while (my($k2, $v2) = each %$value ) 
			 {
				 print "    $k2: $v2\n";
			 }			
		}		
		else
		{
			print "$value\n";
		}
    }
}


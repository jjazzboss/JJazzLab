#!/usr/bin/perl
#########################################################################
#
# Move ResourceBundle key-value pairs between Bundle*.properties files.
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
use File::Temp qw(tempfile);

binmode(STDOUT, ":utf8");          #treat as if it is UTF-8
binmode(STDIN, ":encoding(utf8)"); #actually check if it is UTF-8


sub usage
{
	print "\nUSAGE: $0 [Options] srcBundleFile destBundleDir\n\n";
	print "Move ResourceBundle key-value pairs from srcBundleFile and all its related locale-specific files (Bundle*.properties), into the corresponding ResourceBundle files in destBundleDir.\n\n";	
    print "PARAMETERS:\n";
	print "  srcBundleFile: The source Bundle.properties file. Locale-specific variants (eg Bundle_fr_FR.properties) must be in the same directory.\n";
	print "  destBundleDir: The destination directory where the corresponding ResourceBundle files will be updated (or created).\n";
	print "\nOPTIONS: \n";
	print "  --preview : show the changes to be made without doing them\n";
	print "  --copy : copy the key-value pairs instead of moving them\n";
	print "  --delete : just remove the key-value pairs from srcBundleFile (must be used with --keys and/or --java-files)\n";
	print "  --keys=key1,key2,... : move only pairs key1-value1, key2-value2, etc.\n";
	print "  --keys=\@file : move only keys found in input file (1 key per line)\n";
	print "  --java-files=file1.java,file2.java,... : move only the key-value pairs used by the specified java source files. See getKeysFromJavaLine() function.\n";
	print "  --java-files=\@file : move only the key-value pairs used by the files found in input file (1 java file per line). See getKeysFromJavaLine() function.\n";
	print "\nEXAMPLE: perl $0 --java-files=test.java old/resources/org/xxx/Bundle.properties new/resources/org/yyy\n";
	print "Author: Jerome Lelasseux \@2024\n";	
	exit;
}

# Command line arguments
my $copy=0;
my $delete=0;
my $keysArg=0;
my $javaFiles=0;
my $preview=0;
GetOptions(
	'keys=s' => \$keysArg,
	'java-files=s' => \$javaFiles,	
	'copy' => \$copy,
	'delete' => \$delete,    
	'preview' => \$preview    
) or usage();
usage() unless ($#ARGV == 1);
usage() if ($copy && $delete);
usage() if ($delete && !$keysArg && !$javaFiles);
my ($srcBundleFile, $destDir) = (@ARGV);
die "Source file not found: $srcBundleFile" if (! -f $srcBundleFile) ;
die "srcBundleFile is not Bundle.properties" if (basename($srcBundleFile) ne "Bundle.properties");
die "Destination directory not found: $destDir" if (! -e $destDir) ;


print "============================================================\n";
print "input file     = $srcBundleFile\n";
print "destination dir= $destDir\n";
print "keys argument  = $keysArg\n";
print "java-files arg = $javaFiles\n";
print "move mode      = " . (!$copy && !$delete) ."\n";
print "copy mode      = $copy\n";
print "delete mode    = $delete\n";
print "preview mode   = $preview\n";
print "============================================================\n";

my $srcDir = dirname($srcBundleFile);
my @srcAllBundleFiles = glob($srcDir . "/Bundle*.properties");
my %srcAllKeyValuePairs = getAllKeyValuePairs($srcBundleFile);
my %keyValuePairsToBeMoved = getKeyValuePairsToBeMoved();

if (!%keyValuePairsToBeMoved)
{
	print "Could not find any key-value pair which needs to be moved in $srcBundleFile, exiting.";
	exit;
}


print "\n";
printHash("Impacted key-value pairs ", \%keyValuePairsToBeMoved);
print "\n";

# Process files
foreach my $f (@srcAllBundleFiles)
{
	movePairsToDestFile($f, \%keyValuePairsToBeMoved);
}




#================================================================
# subroutines
#================================================================


# $1 = source BundleResource file
# $2 = hash ref containing the keys to move
sub movePairsToDestFile
{
	my ($srcFile, $pairsHashRef) = @_;
    
	my $destFile = $destDir . "/" . basename($srcFile);		
    print "\n- Processing key-value pairs from $srcFile to $destFile...";
	

	# Get the key-value pairs to be moved from the source file
	my %srcHashAll = getAllKeyValuePairs($srcFile);	
	my %srcHashMoved;
	my %srcHashAllMinusMoved;
	while (my($key, $value) = each %srcHashAll ) 
	{
		if ($pairsHashRef->{$key}) 
		{			
			$srcHashMoved{$key} = $value;
		} else
		{
			$srcHashAllMinusMoved{$key} = $value;
		}
	}
	
	if (%srcHashMoved)
	{
		print "\n";
	}
	else
	{
		print " nothing to do\n";
		return;
	}
	

	if (!$delete)
	{		
		# Add the key-value pairs in dest file	
		
		if (! -f $destFile && !$preview)
		{
			open my $handle, ">", "$destFile" or die "Can't create $destFile\n";
			print $handle "\n";
			close $handle;
		} 


		# Read all the dest file
		my %destHash = getAllKeyValuePairs($destFile);
		
		
		# Add moved keys
		addToHash(\%destHash, \%srcHashMoved);
		
		
		# Rewrite the file
		if (!$preview)
		{
			open my $handle, ">", "$destFile" or die "Can't create $destFile\n";
			foreach my $key (sort keys %destHash)
			{
				print $handle "$key=$destHash{$key}\n";
			}
			close $handle;					
		}
		else
		{
			print "    Writing $destFile :\n";
			foreach my $key (sort keys %destHash)
			{
				print "      $key=$destHash{$key}\n";
			}			
		}
		
	}
	
	
	if (!$copy)
	{

		# Rewrite file without the moved pairs 
		
		if (! $preview)
		{
			open my $handle, ">", "$srcFile" or die "Can't create $srcFile\n";
			foreach my $key (sort keys %srcHashAllMinusMoved)			
			{
				print $handle "$key=$srcHashAllMinusMoved{$key}\n";
			}
			close $handle;		
		}
		else
		{
			print "    Removing key-value pairs from $srcFile :\n";
			foreach my $key (sort keys %srcHashMoved)
			{
				print "      $key=$srcHashMoved{$key}\n";
			}			
			
		}
		
		if (-z $srcFile)
		{
			# File is empty, delete it
			print "    Deleting empty source file $srcFile\n";
			if (!$preview)
			{
				unlink $srcFile or die "Error while deleting file $srcFile";
			}
		}
		
	}
	
}


# Extract the key-value pairs to be moved, depending on the command-line parameters
# Return an hash
sub getKeyValuePairsToBeMoved
{
	my %hash;

	# keys parameter specified
	if (substr($keysArg, 0, 1) eq "@")
	{
		# keys are to be read from a dedicated input file
		my $inputfile=substr($keysArg, 1);
		open my $handle, '<', $inputfile or die "Cannot open $inputfile for reading: $!";	
		while (<$handle>)
		{
			chomp;	
            my $key = trim($_);
			my $value = $srcAllKeyValuePairs{$key};
			$hash{$key} = $value if defined $value;
		}
		close $handle;		
		
	} elsif ($keysArg)
	{
		# keys are specified as command line parameter
		my @keys = split(',', $keysArg);
		foreach my $key (@keys) 
		{
			my $value = $srcAllKeyValuePairs{$key};
			$hash{$key} = $value if defined $value;
		}
	}
	
	# java-files parameter specified: keys need to be extracted from java files
	if (substr($javaFiles, 0, 1) eq "@")
	{
		# java files are to be read from a dedicated input file
		my $inputfile=substr($javaFiles, 1);
		open my $handle, '<', $inputfile or die "Cannot open $inputfile for reading: $!";	
		while (<$handle>)
		{
			chomp;		
			my $javaFile = trim($_);
			my %fileKeys = processJavaFile($javaFile);
			addToHash(\%hash, \%fileKeys);
		}
		close $handle;		
		
	} elsif ($javaFiles)
	{
		# java files are directly specified as command-line parameter
		my @files = split(',', $javaFiles);
		foreach my $javaFile (@files)
		{					
			my %fileKeys = processJavaFile($javaFile);
            addToHash(\%hash, \%fileKeys);
		}
	} 


	# no parameters specified
	if (!$keysArg && !$javaFiles)
	{
		# copy all the key-value pairs
		addToHash(\%hash, \%srcAllKeyValuePairs);
	}
		

    return %hash;
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
		print "WARNING unparsable ResUtil.getString(...) detected, please fix : $line\n";
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
	open my $handle, '<', $javaFile or die "Cannot open $javaFile for reading: $!";	
	while (<$handle>)
	{
		chomp;	
		my @keys = getKeysFromJavaLine($_);
		for my $key (@keys)
		{
			my $value = $srcAllKeyValuePairs{$key};
			$res{$key} = $value if defined $value;
		}					
	}
	close $handle;	
	
	return %res;
}

# $1 ResourceBundle file name
# returns: hash with all key-value pairs
sub getAllKeyValuePairs
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
			my $eqIndex = index($_, "=");
			my $key = substr($_, 0, $eqIndex);
			my $val = substr($_, $eqIndex+1);
			$res{$key} = $val;
		}
		close $handle;	
	}	
    return %res;	
}




sub trim 
{
   return $_[0] =~ s/^\s+|\s+$//rg;
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
        print "   $key = $value\n";
    }
}

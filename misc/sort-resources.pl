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


binmode(STDOUT, ":utf8");          # treat as if it is UTF-8
binmode(STDIN, ":encoding(utf8)"); # actually check if it is UTF-8


sub trim 
{ 
	my $s = shift; 
	$s =~ s/^\s+|\s+$//g; 
	return $s; 
}


sub usage
{
	print "\nUSAGE: $0 [options] maven_project_dir1 [maven_project_dir2] ... \n";
	print "Find all ResourceBundle properties files in the Maven project dir(s) and sort their content per key.\n";
    print "PARAMETERS:\n";
	print "  maven_project_dir: a maven standard project directory which must contain a pom.xml file at its root.\n";
	print "OPTIONS: \n";
	print "  --preview : just display sorted files on stdout\n";	
	print "AUTHOR: Jerome Lelasseux \@2024\n";	
	exit;
}

# Command line arguments
my $preview=0;
GetOptions(
	'preview' => \$preview,
) or usage();
usage() if ($#ARGV < 0);
	



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
	my $resDir="$projectDir/src/main/resources";
	
	print "== ============================================================\n";
	print "PROCESSING $projectDir... ";
	
	if (! -d $projectDir || ! -f "$projectDir/pom.xml" || ! -d "$projectDir/src/main/resources")
	{
		print "skipped - invalid Maven project dir\n";
		return;
	}

	print "\n";
				
	my $count=0;
	
	find ( sub {
				return unless -f;       
				return unless /^Bundle.*properties$/;  
				my $relPath=File::Spec->abs2rel($File::Find::dir, $resDir);						
				my $fn=$_;
				my $relFn="$relPath/$_";				
				$count++;
				my %keyValuePairs=processBundleFile($_);		
			
				if (! %keyValuePairs)
				{
					print "###### WARNING empty file : $relFn\n";		
					return;					
				}
			
				# Rewrite file with keys sorted
				if (!$preview)
				{
					open my $handle, ">", "$fn" or die "Can't create $relFn\n";
					foreach my $key (sort keys %keyValuePairs)
					{		
						print $handle "$key=$keyValuePairs{$key}\n";
					}						
					close $handle;					
				} else
				{
					print "$relFn >>>\n";
					foreach my $key (sort keys %keyValuePairs)
					{		
						print "  $key = $keyValuePairs{$key}\n";
					}				
				}
				
			}, 
		$resDir );	
		
	print "$count files processed\n";
	
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
				print "###### WARNING Possible Java U-based unicode escapes found: $_\n" if (/u00/);					
				my $eqIndex = index($_, "=");
				my $key = trim(substr($_, 0, $eqIndex));
				my $val = substr($_, $eqIndex+1);
				$res{$key} = $val;
		}
		close $handle;	
	}	
    return %res;	
}


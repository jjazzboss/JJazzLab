use warnings;
use strict;

use Getopt::Long qw(GetOptions);

sub usage
{
	print "USAGE: $0 [OPTIONS] module_code_base  modules_project_xml_files \n";
	print "List module dependencies and/or find all (transitive) dependencies between 2 given modules.\n";
	print "Author: Jerome Lelasseux \@2021\n";
	print "OPTIONS: \n";
	print " --test-dep=<test_module_code_base> : search if module_code_base is transitively dependent of test_module_code_base, if yes show the dependency chains.\n";
	print " --print     : print all dependencies\n";
	exit;
}

# Command line arguments
my $printAll=0;
my $testDep;
GetOptions(
	'test-dep=s' => \$testDep,
	'print' => \$printAll,
) or usage();


my $targetModule = shift or die usage();

# First collect all modules dependencies
my %mapModuleDeps;

foreach my $file (@ARGV)
{
	my $first = 1;
	my $module;
	my @deps;
	open FILE , '<'.$file  or die $!;
	while( <FILE> ) 
	{ 
		if (/.*<code-name-base>([a-zA-Z0-9._]+)<\/code-name-base>.*/)
		{
			if ($first)
			{
				$module = $1;
				$first = 0;
			} elsif (index($1,"jjazz") >= 0)
			{
				push(@deps, $1);
			}
		}
	} 	
	
	$mapModuleDeps{$module} = [ @deps ];
	
	close FILE;
}



if ($printAll)
{
	for my $module (keys %mapModuleDeps)
	{
		#print(">> $module: @{ $mapModuleDeps{$module} }\n");
		print(">> $module:\n");
		for my $dep (@{ $mapModuleDeps{$module} })
		{
			print "     $dep\n";
		}
				
	}
	print "\n\n";
}

die "$targetModule not found in the analyzed modules" if !exists $mapModuleDeps{$targetModule};

die "No --test-dep option used" if (!$testDep);


my $chain;

print "Target module: $targetModule\n";
print "Testing dependency to module: $testDep\n";
my $res;
my $foundDep;
do
{
	$res = processModule($targetModule);
	if (defined($res))
	{
		print "RESULT : $res\n";
		$foundDep=1;
	}
} while (defined($res));

if (!$foundDep)
{
	print "No dependency found\n";
}

sub processModule
{
	my ($module) = @_;
	if (!defined($mapModuleDeps{$module}))
	{
		return undef;
	}
	my @deps = @{ $mapModuleDeps{$module} };
	for my $m (@deps)
	{
		if ($m eq $testDep)
		{
			return $m;
		}
	}
	for my $m (@deps)
	{
		my $res = processModule($m);
		if (defined($res))
		{
			delete $mapModuleDeps{$m};
			return "$m > $res";
		}
	}
	return undef;
}
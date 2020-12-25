# Apply native2ascii on each Bundle*.properties to make sure it is encoded in ISO8859-1

function usage()
{
    echo "USAGE: UTF8toISO directory"
	echo "Apply native2ascii to all Bundle*.properties from current directory and subdirectories."
    exit
}

# $1 property file
function process()
{
	newfile="$1.tmp"
	mv "$1" "$newfile"
	if [[ ! $? -eq 0 ]]
	then
		exit 1
	fi
	
	native2ascii -encoding UTF-8 "$newfile" "$1"
	if [[ ! $? -eq 0 ]]
	then
		exit 1
	fi
	
	rm "$newfile"
	echo "Encoded UTF-8 to ISO-8859-1: $1"	
}

if [[ ! $# -eq 1 ]]
then
	usage
fi

if [[ ! -d "$1" ]] 
then
	echo "Invalid directory: $1"
	exit
fi

cd "$1"

files=( $(find . -name "Bundle*.properties") );		# Make a list. White-space in files/directories OK.
for file in "${files[@]}"; do
	process "$file"
done




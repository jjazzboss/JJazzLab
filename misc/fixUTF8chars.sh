#!/bin/bash
# Replace latin-based accents as java U-based hexadecimal escapes (a.k.a. unicode escapes) like \u00E1 into the real char

	
# All single latin accented chars
# $1  filename
function process () 
{

	if [ "$inplace" = true ] ; then
		echo "Inplace processing $1..."
	else
		echo "Processing $1..."
	fi	
	tmpFile=$(mktemp)	
	sed 's/\\u00c3\\u0080/À/g' $1 \
	| sed 's/\\u00c3\\u0081/Á/g' \
	| sed 's/\\u00c3\\u0082/Â/g' \
	| sed 's/\\u00c3\\u0083/Ã/g' \
	| sed 's/\\u00c3\\u0084/Ä/g' \
	| sed 's/\\u00c3\\u0085/Å/g' \
	| sed 's/\\u00c3\\u0086/Æ/g' \
	| sed 's/\\u00c3\\u0087/Ç/g' \
	| sed 's/\\u00c3\\u0088/È/g' \
	| sed 's/\\u00c3\\u0089/É/g' \
	| sed 's/\\u00c3\\u008a/Ê/g' \
	| sed 's/\\u00c3\\u008b/Ë/g' \
	| sed 's/\\u00c3\\u008c/Ì/g' \
	| sed 's/\\u00c3\\u008d/Í/g' \
	| sed 's/\\u00c3\\u008e/Î/g' \
	| sed 's/\\u00c3\\u008f/Ï/g' \
	| sed 's/\\u00c3\\u0090/Ð/g' \
	| sed 's/\\u00c3\\u0091/Ñ/g' \
	| sed 's/\\u00c3\\u0092/Ò/g' \
	| sed 's/\\u00c3\\u0093/Ó/g' \
	| sed 's/\\u00c3\\u0094/Ô/g' \
	| sed 's/\\u00c3\\u0095/Õ/g' \
	| sed 's/\\u00c3\\u0096/Ö/g' \
	| sed 's/\\u00c3\\u0097/×/g' \
	| sed 's/\\u00c3\\u0098/Ø/g' \
	| sed 's/\\u00c3\\u0099/Ù/g' \
	| sed 's/\\u00c3\\u009a/Ú/g' \
	| sed 's/\\u00c3\\u009b/Û/g' \
	| sed 's/\\u00c3\\u009c/Ü/g' \
	| sed 's/\\u00c3\\u009d/Ý/g' \
	| sed 's/\\u00c3\\u009e/Þ/g' \
	| sed 's/\\u00c3\\u009f/ß/g' \
	| sed 's/\\u00c3\\u00a0/à/g' \
	| sed 's/\\u00c3\\u00a1/á/g' \
	| sed 's/\\u00c3\\u00a2/â/g' \
	| sed 's/\\u00c3\\u00a3/ã/g' \
	| sed 's/\\u00c3\\u00a4/ä/g' \
	| sed 's/\\u00c3\\u00a5/å/g' \
	| sed 's/\\u00c3\\u00a6/æ/g' \
	| sed 's/\\u00c3\\u00a7/ç/g' \
	| sed 's/\\u00c3\\u00a8/è/g' \
	| sed 's/\\u00c3\\u00a9/é/g' \
	| sed 's/\\u00c3\\u00aa/ê/g' \
	| sed 's/\\u00c3\\u00ab/ë/g' \
	| sed 's/\\u00c3\\u00ac/ì/g' \
	| sed 's/\\u00c3\\u00ad/í/g' \
	| sed 's/\\u00c3\\u00ae/î/g' \
	| sed 's/\\u00c3\\u00af/ï/g' \
	| sed 's/\\u00c3\\u00b0/ð/g' \
	| sed 's/\\u00c3\\u00b1/ñ/g' \
	| sed 's/\\u00c3\\u00b2/ò/g' \
	| sed 's/\\u00c3\\u00b3/ó/g' \
	| sed 's/\\u00c3\\u00b4/ô/g' \
	| sed 's/\\u00c3\\u00b5/õ/g' \
	| sed 's/\\u00c3\\u00b6/ö/g' \
	| sed 's/\\u00c3\\u00b8/ø/g' \
	| sed 's/\\u00c3\\u00b9/ù/g' \
	| sed 's/\\u00c3\\u00ba/ú/g' \
	| sed 's/\\u00c3\\u00bb/û/g' \
	| sed 's/\\u00c3\\u00bc/ü/g' \
	| sed 's/\\u00c3\\u00bd/ý/g' \
	| sed 's/\\u00c3\\u00bf/ÿ/g' \
	| sed 's/\\u00c2\\u00bf/¿/g' \
	| sed 's/\\u00c2\\u00a1/¡/g' \
	> $tmpFile

	if [ "$inplace" = true ] ; then
		cp $tmpFile $1
	else
		cat $tmpFile
	fi
	rm $tmpFile
}

if [ $# -eq 0 ]
then
	echo "USAGE: fixUTF8chars [-i] files"
	echo "Replace faulty strings in files and send result on the standard output."
	echo "-i: inplace replace in files"
	exit 1
fi


if [ "$1" == "-i" ]
then
	inplace=true
	shift
fi
filenames=$*


for file in $filenames
do
	process $file
done













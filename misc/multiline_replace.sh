# multiline text replace in pom.xml with perl 
for file in $*
do
	echo "Updating $file dependency"
	file="${file,,}"		 # lowercase
	f2=`basename $file`
	find . -name pom.xml -exec perl -i -pe "BEGIN{undef \$/;} s!<groupId>[^<]+</groupId>(\n*)(\s*)<artifactId>$f2<!<groupId>org.jjazzlab.core</groupId>\$1\$2<artifactId>$f2<!sm" {} \;
done
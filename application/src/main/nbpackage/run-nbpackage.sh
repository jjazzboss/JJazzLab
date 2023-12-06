#!/bin/sh
#
# Run nbpackage on JJazzLab/application/<file>.zip
# Must be run from JJazzLabExtra/target/deployment
#

# Relative paths from jjazzlab/application/target  (.zip app file location)
NBPACKAGE="../../../netbeans-nbpackage/target/nbpackage-1.0-beta4-SNAPSHOT/bin/nbpackage"
INNOPROPERTIES="inno.properties"
ZIPFILE="../../../JJazzLab/application/target/${brandingToken}-${project.version}.zip"


if [ ! -e $ZIPFILE  ]
then
	echo "File not found: $ZIPFILE"
        echo "Did you run the JJazzLab custom Maven goal for preparing the .zip file in jjazzlab-app ?"
	exit
fi
if [ ! -e $NBPACKAGE  ]
then
	echo "File not found: $NBPACKAGE"
	exit
fi
if [ ! -e $INNOPROPERTIES  ]
then
	echo "File not found: $INNOPROPERTIES"
	exit
fi



set -x  
"$NBPACKAGE" -v --config "$INNOPROPERTIES" --input "$ZIPFILE"





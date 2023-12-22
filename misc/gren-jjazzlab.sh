if [ ! -d misc ]
then
    echo "Invalid start directory. Must be run at jjazzlab project root dir."
    exit
fi
if [[ $# = 0 ]]
then 
    echo "EXAMPLE USAGE: misc/gren-jjazzlab.sh 4.0.2"
    exit
fi
gren release --override -c misc/gren.cfg --only-milestones --milestone-match="$1"




	 

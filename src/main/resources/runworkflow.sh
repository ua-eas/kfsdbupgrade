# run workflow ingestion
pathToProperties=/security/uaf/dbupgrade/kfsdbupgrade.properties

#if a command line argument is added, assume it is a path to
#  the .properties file to use instead of the default
if [[ $1 ]]; then
	echo "Using argument as path to .properties file: $1"
	pathToProperties=$1
fi

if [ -f nohup.out ]; then
	rm nohup.out
fi

#cd src/main/resources

nohup java -Xmx1g -jar target/kfsdbupgrade.jar $pathToProperties ingestWorkflow

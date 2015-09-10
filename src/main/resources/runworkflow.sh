# run workflow ingestion
DBUPGRADE_FILE_PATH=/security/uaf/dbupgrade

if [ -f nohup.out ]; then
	rm nohup.out
fi

cd src/main/resources

nohup java -Xmx1g -jar "$WORKSPACE/target/kfsdbupgrade.jar" "$DBUPGRADE_FILE_PATH/kfsdbupgrade.properties" ingestWorkflow
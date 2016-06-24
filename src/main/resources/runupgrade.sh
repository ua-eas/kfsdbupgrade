# run database upgrade scripts
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

nohup java -Xmx1g -jar target/kfsdbupgrade.jar $pathToProperties

# the command below will not work unless we get property filtering to work
#nohup java -Xmx1g -Dua.kfs.dbupgrade.target.db.user=$UA_KFS_DBUPGRADE_TARGET_DB_USER \
#-Dua.kfs.dbupgrade.target.db.pw=$UA_KFS_DBUPGRADE_TARGET_DB_PW \
#-Dua.kfs.dbupgrade.legacy.db.user=$UA_KFS_DBUPGRADE_LEGACY_DB_USER \
#-Dua.kfs.dbupgrade.legacy.db.pw=$UA_KFS_DBUPGRADE_LEGACY_DB_PW \
#-Dua.kfs.dbupgrade.maint.doc.encryption.key=$UA_KFS_DBUPGRADE_MAINT_DOC_ENCRYPTION_KEY \
#-jar target/kfsdbupgrade.jar src/main/resources/kfsdbupgrade.properties &

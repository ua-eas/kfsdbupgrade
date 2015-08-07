if [ -f nohup.out ]; then
	rm nohup.out
fi

# will not work unless we get property filtering to work
nohup java -Xmx1g -Dua.kfs.dbupgrade.target.db.user=$UA_KFS_DBUPGRADE_TARGET_DB_USER \
-Dua.kfs.dbupgrade.target.db.pw=$UA_KFS_DBUPGRADE_TARGET_DB_PW \
-Dua.kfs.dbupgrade.legacy.db.user=$UA_KFS_DBUPGRADE_LEGACY_DB_USER \
-Dua.kfs.dbupgrade.legacy.db.pw=$UA_KFS_DBUPGRADE_LEGACY_DB_PW \
-Dua.kfs.dbupgrade.maint.doc.encryption.key=$UA_KFS_DBUPGRADE_MAINT_DOC_ENCRYPTION_KEY \
-jar target/kfsdbupgrade.jar src/main/resources/kfsdbupgrade.properties &


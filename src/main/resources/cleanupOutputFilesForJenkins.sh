# cleanup log files from previous run
UPGRADE_LOG_PATH=$JENKINS_HOME/uaf/dbupgrade/logs

cd $UPGRADE_LOG_PATH

if [ -f kfs-database-upgrade.log ]; then
  rm kfs-database-upgrade.log
fi

if [ -f kfs-database-upgrade-processed-files.txt ]; then
   rm kfs-database-upgrade-processed-files.txt
fi
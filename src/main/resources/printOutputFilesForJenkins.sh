# print output files
UPGRADE_LOG_PATH=.
echo $UPGRADE_LOG_PATH

echo '----'
echo 'kfs-database-upgrade-processed-files.txt contents'
echo '---------------------------------'
cat $UPGRADE_LOG_PATH/kfs-database-upgrade-processed-files.txt

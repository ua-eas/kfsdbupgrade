-- ======================================================================================================
-- Changes needed to support the upgrade from liquibase 1.9.5 to 3.3.5 (UAF-454)
-- ======================================================================================================
ALTER TABLE KULOWNER.DATABASECHANGELOG ADD ORDEREXECUTED INT;
UPDATE KULOWNER.DATABASECHANGELOG SET ORDEREXECUTED = -1;
ALTER TABLE KULOWNER.DATABASECHANGELOG MODIFY ORDEREXECUTED INT NOT NULL;
ALTER TABLE KULOWNER.DATABASECHANGELOG ADD EXECTYPE VARCHAR(10);
UPDATE KULOWNER.DATABASECHANGELOG SET EXECTYPE = 'EXECUTED';
ALTER TABLE KULOWNER.DATABASECHANGELOG MODIFY EXECTYPE VARCHAR(10) NOT NULL;
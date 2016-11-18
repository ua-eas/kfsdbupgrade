-- =======================================================================================================================
-- UAF-3618 : Add Parm Values to DB Upgrade
-- =======================================================================================================================
UPDATE KRCR_NMSPC_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';

UPDATE KRCR_DRVD_CMPNT_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD='KUALI-TAX';

UPDATE KRCR_CMPNT_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';

UPDATE KRCR_PARM_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';

UPDATE KRCR_PARM_T
SET APPL_ID='KFS'
WHERE NMSPC_CD = 'KFS-TAX';

UPDATE KRIM_ROLE_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';

UPDATE KRIM_RSP_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';

UPDATE KRIM_PERM_T
SET NMSPC_CD='KFS-TAX'
WHERE NMSPC_CD = 'KUALI-TAX';
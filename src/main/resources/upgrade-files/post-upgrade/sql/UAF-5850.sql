--------------------------------------------------------------------------------
--   UAF-5850 : Delete Scrapped 3.0 Parameter PRINCIPAL_IDS & Parameter Component RefreshActionListStep. (Subtask of UAF-92)
--------------------------------------------------------------------------------
DELETE FROM KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND PARM_NM = 'PRINCIPAL_IDS';
DELETE FROM KRCR_CMPNT_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'RefreshActionListStep'; 

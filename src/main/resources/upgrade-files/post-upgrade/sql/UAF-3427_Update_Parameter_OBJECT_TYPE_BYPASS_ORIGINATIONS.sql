-- =======================================================================================================================
-- UAF-3427 : Update Parameter OBJECT_TYPE_BYPASS_ORIGINATIONS in Upgrade Scripts. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.KRCR_PARM_T SET VAL = '01;BO' WHERE CMPNT_CD = 'ScrubberStep' AND PARM_NM = 'OBJECT_TYPE_BYPASS_ORIGINATIONS';
-- =======================================================================================================================
-- UAF-4059 : Parameter changes to implement Chart code autofill. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.KRCR_PARM_T SET VAL = 'Y' WHERE PARM_NM='ACCOUNTS_CAN_CROSS_CHARTS_IND';
UPDATE KULOWNER.KRCR_PARM_T SET VAL = 'UA' WHERE PARM_NM='DEFAULT_CHART_CODE';
UPDATE KULOWNER.KRCR_PARM_T SET VAL = '1' WHERE PARM_NM='DEFAULT_CHART_CODE_METHOD';
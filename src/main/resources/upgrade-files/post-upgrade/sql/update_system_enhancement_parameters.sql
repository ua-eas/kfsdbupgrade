-- =============================================================================================================
-- (UAF-2924) UAF-2924 : Update Parameters related to system enhancements (UAF-92)
-- =============================================================================================================
UPDATE KULOWNER.KRCR_PARM_T SET VAL = 'Y' WHERE PARM_NM = 'UPDATE_TOTAL_AMOUNT_IN_POST_PROCESSING_IND';
UPDATE KULOWNER.KRCR_PARM_T SET VAL = NULL WHERE PARM_NM = 'RUN_DATE';
UPDATE KULOWNER.KRCR_PARM_T SET VAL = NULL WHERE PARM_NM = 'RUN_DATE_CUTOFF_TIME'; 
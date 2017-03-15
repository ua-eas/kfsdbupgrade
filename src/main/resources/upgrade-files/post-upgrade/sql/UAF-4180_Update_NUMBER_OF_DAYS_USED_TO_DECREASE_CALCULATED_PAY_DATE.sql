-- =======================================================================================================================
--  UAF-4180 : Update New 6.0 Parameter NUMBER_OF_DAYS_USED_TO_DECREASE_CALCULATED_PAY_DATE. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.KRCR_PARM_T SET VAL = '0' WHERE PARM_NM='NUMBER_OF_DAYS_USED_TO_DECREASE_CALCULATED_PAY_DATE';
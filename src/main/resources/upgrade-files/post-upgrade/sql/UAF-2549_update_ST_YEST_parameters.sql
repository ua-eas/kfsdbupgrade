-- =======================================================================================================================
-- (UAF-2549) : Update ST and YEST Parameters in Upgrade Scripts. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.KRCR_PARM_T SET CMPNT_CD = 'Document'  WHERE NMSPC_CD = 'KFS-LD' AND CMPNT_CD = 'SalaryExpenseTransfer' AND PARM_NM = 'DEFAULT_NUMBER_OF_FISCAL_PERIODS_ERROR_CERTIFICATION_TAB_REQUIRED';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-LD' AND CMPNT_CD = 'YearEndSalaryExpenseTransfer' AND PARM_NM = 'DEFAULT_NUMBER_OF_FISCAL_PERIODS_ERROR_CERTIFICATION_TAB_REQUIRED';
UPDATE KULOWNER.KRCR_PARM_T SET CMPNT_CD = 'Document'  WHERE NMSPC_CD = 'KFS-LD' AND CMPNT_CD = 'SalaryExpenseTransfer' AND PARM_NM = 'ERROR_CERTIFICATION_DEFAULT_OVERRIDE_BY_SUB_FUND';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-LD' AND CMPNT_CD = 'YearEndSalaryExpenseTransfer' AND PARM_NM = 'ERROR_CERTIFICATION_DEFAULT_OVERRIDE_BY_SUB_FUND';

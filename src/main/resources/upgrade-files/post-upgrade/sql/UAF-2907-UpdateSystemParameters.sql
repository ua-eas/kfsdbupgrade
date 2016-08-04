-- ============================================================================
-- Update SYSTEM Parameters in Upgrade Scripts (UAF-2907) subtask of (UAF-92)
-- ============================================================================
UPDATE KRCR_PARM_T SET VAL = 'N' WHERE CMPNT_CD = 'Document' AND PARM_NM = 'ENABLE_FISCAL_PERIOD_SELECTION_IND';

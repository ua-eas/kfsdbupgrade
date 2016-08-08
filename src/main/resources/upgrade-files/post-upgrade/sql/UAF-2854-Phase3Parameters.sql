-- ============================================================================
-- Update Phase 3 Parameters in Upgrade Scripts (UAF-2854) subtask of (UAF-92)
-- ============================================================================
UPDATE KRCR_PARM_T SET VAL='UE;MF' WHERE CMPNT_CD='LedgerEntry' AND PARM_NM='LINK_DOCUMENT_NUMBER_TO_LABOR_ORIGIN_CODES';

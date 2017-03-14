-- =============================================================================
-- UAF-4177 : Update parameters for Cash Receipt. Subtask of (UAF-92)
-- =============================================================================
UPDATE KULOWNER.KRCR_PARM_T SET VAL = 'N' WHERE PARM_NM='DISPLAY_CASH_RECEIPT_DENOMINATION_DETAIL_IND';
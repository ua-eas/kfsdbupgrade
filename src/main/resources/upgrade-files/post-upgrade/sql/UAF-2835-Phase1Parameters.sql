-- ============================================================================
-- Update Phase 1 Parameters in Upgrade Scripts (UAF-2835) subtask of (UAF-92)
-- ============================================================================
UPDATE KRCR_PARM_T SET VAL = 'FDBL;ASST;PERS;TRSF' WHERE CMPNT_CD = 'AdvanceDeposit' AND PARM_NM = 'OBJECT_CONSOLIDATIONS';
UPDATE KRCR_PARM_T SET VAL = 'CA;BU;FB;PL;MT;CE;FR;HW;RE;SA;VA;TN;CX' WHERE CMPNT_CD = 'AdvanceDeposit' AND PARM_NM = 'OBJECT_SUB_TYPES';
UPDATE KRCR_PARM_T SET VAL = 'BB;CB' WHERE CMPNT_CD = 'JournalVoucher' AND PARM_NM = 'BUDGET_BALANCE_TYPES';
UPDATE KRCR_PARM_T SET VAL = 'FDBL;ASST;PERS;TRSF' WHERE CMPNT_CD = 'PreEncumbrance' AND PARM_NM = 'OBJECT_CONSOLIDATIONS';

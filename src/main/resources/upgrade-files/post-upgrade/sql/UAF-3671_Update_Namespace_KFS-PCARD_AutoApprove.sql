-- =======================================================================================================================
-- UAF-3671 : Update Namespace to KFS-PCARD for Parameter Componenent ProcurementCardAutoApproveDocumentsStep. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE "KULOWNER"."KRCR_DRVD_CMPNT_T" SET NMSPC_CD = 'KFS-PCARD'  WHERE CMPNT_CD = 'ProcurementCardAutoApproveDocumentsStep';
UPDATE "KULOWNER"."KRCR_CMPNT_T" SET NMSPC_CD = 'KFS-PCARD'  WHERE CMPNT_CD = 'ProcurementCardAutoApproveDocumentsStep';


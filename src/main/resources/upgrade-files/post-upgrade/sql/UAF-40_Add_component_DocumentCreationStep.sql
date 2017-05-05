-- =======================================================================================================================
-- UAF-40 : Add component in KRCR_CMPNT_T and KRCR_DRVD_CMPNT_T for DocumentCreationStep
-- =======================================================================================================================
UPDATE "KULOWNER"."KRCR_DRVD_CMPNT_T" SET NMSPC_CD = 'KFS-PCARD'  WHERE CMPNT_CD = 'ProcurementCardAutoApproveDocumentsStep';
UPDATE "KULOWNER"."KRCR_CMPNT_T" SET NMSPC_CD = 'KFS-PCARD'  WHERE CMPNT_CD = 'ProcurementCardAutoApproveDocumentsStep';

INSERT INTO "KULOWNER"."KRCR_CMPNT_T" (NMSPC_CD, CMPNT_CD, NM, ACTV_IND, OBJ_ID, VER_NBR)
VALUES ('KFS-FP', 'DocumentCreationStep', 'Document Creation Step', 'Y', sys_guid(), 1);


INSERT INTO "KULOWNER"."KRCR_DRVD_CMPNT_T" (NMSPC_CD, CMPNT_CD, NM, CMPNT_SET_ID)
VALUES ('KFS-FP', 'DocumentCreationStep', 'DocumentCreationStep', 'STEP:KFS');
INSERT INTO "KULOWNER"."KRCR_DRVD_CMPNT_T" (NMSPC_CD, CMPNT_CD, NM, CMPNT_SET_ID)
VALUES ('KFS-FP', 'BankTransactionFilesValidationStep', 'BankTransactionFilesValidationStep', 'STEP:KFS');
INSERT INTO "KULOWNER"."KRCR_DRVD_CMPNT_T" (NMSPC_CD, CMPNT_CD, NM, CMPNT_SET_ID)
VALUES ('KFS-FP', 'BankTransactionFilesConsolidateStep', 'BankTransactionFilesConsolidateStep', 'STEP:KFS');
-- =======================================================================================================================
-- (UAF-2471) UAF-2471 : Update (PMSO) Requisition Source Maintenance Table Values in Database Script. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.PUR_REQS_SRC_T SET ALLOW_COPY_DAYS = 60 WHERE REQS_SRC_CD = 'B2B' AND REQS_SRC_DESC = 'B2B';
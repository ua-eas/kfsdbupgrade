-- =============================================================================================================
-- (UAF-3856) : Update View Batch Job (#10491) permission to be in the KFS-TAX namespace
-- =============================================================================================================


UPDATE KULOWNER.KRIM_PERM_T SET nmspc_cd='KFS-TAX' WHERE PERM_ID='10491';
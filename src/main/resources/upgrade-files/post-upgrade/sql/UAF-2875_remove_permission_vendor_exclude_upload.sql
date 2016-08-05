-- =======================================================================================================================
-- (UAF-2875) : Remove Permission 'Use Vendor Exclude File Upload Screen' From Reviewer Role Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.KRIM_ROLE_PERM_T SET ACTV_IND='N' WHERE PERM_ID='KFSCNTRB162-PRM' AND ROLE_ID='56';
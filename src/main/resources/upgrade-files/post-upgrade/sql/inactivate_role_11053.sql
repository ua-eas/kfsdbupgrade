-- =============================================================================================================
-- (UAF-2321) Inactivate Redundant Role 11053 <Exclude Single Actor Separation Of Duties> in Upgrade Scripts (UAF-92)
-- =============================================================================================================
update kulowner.KRIM_ROLE_T set ACTV_IND = 'N' where ROLE_ID ='11053';
update kulowner.KRIM_ROLE_RSP_T set ACTV_IND = 'N'  where ROLE_ID ='11053';
update kulowner.KRIM_ROLE_MBR_T set ACTV_TO_DT = '01-JAN-16' where ROLE_ID = '11053';
---- =============================================================================================================
-- UAF-5231 : KFS7 STG Roles Will Not Load
-- Description of issue: role_id=1305 can't load for edit due to can't retrieve role type service.
--			role type service for role_id=1305 is derived role type service which should be published by KFS.
--			To retrieve derived role type service published by KFS, it needs to be declared as KSB published service name.
-- Fix: update KRIM_TYP_T for kim_typ_id = '1102' to use KSB published service name.
-- =============================================================================================================
UPDATE KRIM_TYP_T SET SRVC_NM='{http://kfs.kuali.org/kfs/v5_0}objectSubTypeCodeRoleTypeService' WHERE KIM_TYP_ID = '1102';
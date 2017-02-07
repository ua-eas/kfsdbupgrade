-- =======================================================================================================================
-- UAF-3846 : Removal of responsibilities as per discontinue of matching mod. Subtask of (UAF-92)
-- AKA  UAF-3846 : Remove retired responsibilities
-- =======================================================================================================================
--To remove the attributes for the responsibilities 10380, 10381, 10382
DELETE FROM KRIM_RSP_ATTR_DATA_T WHERE RSP_ID IN ('10380', '10381', '10382');
--To remove the action item on the role - resonsibilty relationship
DELETE FROM KRIM_ROLE_RSP_ACTN_T WHERE ROLE_RSP_ID IN ('10403','10404','10405');
--To remove the role - responsibilty relationship
DELETE FROM KRIM_ROLE_RSP_T WHERE RSP_ID IN ('10380', '10381', '10382');
--To remove the responsibilities 10380, 10381, 10382 from the main responsibility table
DELETE FROM KRIM_RSP_T WHERE RSP_ID IN ('10380', '10381', '10382');

-- =============================================================================================================
-- (UAF-2070) Add "Lookup Records" Permission to the KFS-PDP UA Check Control Role in DB Upgrade Script (UAF-92)
-- =============================================================================================================
insert into kulowner.KRIM_ROLE_PERM_T (ROLE_PERM_ID, OBJ_ID, VER_NBR, ROLE_ID, PERM_ID, ACTV_IND) VALUES (KRIM_ROLE_PERM_ID_S.nextval, sys_guid(), '1', '10430', 'KFSMI6886-PRM2', 'Y');

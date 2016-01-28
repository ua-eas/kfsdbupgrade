-- ======================================================================================================
-- Missing 'Administer Routing for Document KFST' Permission to be added (UAF-1861)
-- ======================================================================================================
insert into kulowner.KRIM_ROLE_PERM_T (ROLE_PERM_ID, OBJ_ID, VER_NBR, ROLE_ID, PERM_ID, ACTV_IND)
VALUES ('5370', sys_guid(), '1', '11513', '12000', 'Y');
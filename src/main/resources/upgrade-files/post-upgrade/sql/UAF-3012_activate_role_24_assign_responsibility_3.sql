-- =============================================================================================================
-- (UAF-3012) : Activate Role #24 <Content Reviewer> and assign Responsibility #3 <Review REQS Organization> to the Role. Subtask of (UAF-92)
-- =============================================================================================================
-- 1)  activate Role #24 <Content Reviewer> 
UPDATE KULOWNER.KRIM_ROLE_T SET  ACTV_IND = 'Y'  WHERE ROLE_ID = '24';
-- 2) assign Responsibility #3 <Review REQS Organization> to the Role
INSERT INTO KULOWNER.KRIM_ROLE_RSP_T (ROLE_RSP_ID,	OBJ_ID,	VER_NBR,	ROLE_ID,	RSP_ID,	ACTV_IND) VALUES (KRIM_ROLE_RSP_ID_S.nextval, sys_guid(), '1','24','3','Y');

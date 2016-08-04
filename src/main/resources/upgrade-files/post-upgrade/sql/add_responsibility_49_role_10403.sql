-- =======================================================================================================================
-- UAF-2739 : Activate Responsibility 49 and Add Responsibility 49 (Review KFS Tax) to Role 10403 (UA FSO AP Tax Specialists). Subtask of (UAF-92)
-- =======================================================================================================================
--The below SQL is to delete the extra 5 detail values that came over from 3.0:
DELETE FROM KULOWNER.KRIM_RSP_ATTR_DATA_T WHERE ATTR_DATA_ID IN ('165178','157','158','159','160');

--The responsibility is actually active in 6.0, but the responsibility role connection in the KRIM_ROLE_RSP_T table is inactive. The below SQL is to make that active:
UPDATE KULOWNER.KRIM_ROLE_RSP_T SET ACTV_IND = 'Y' WHERE ROLE_ID='10403';
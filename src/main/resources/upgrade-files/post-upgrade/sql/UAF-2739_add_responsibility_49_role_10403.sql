-- =======================================================================================================================
-- UAF-2739 : Activate Responsibility 49 and Add Responsibility 49 (Review KFS Tax) to Role 10403 (UA FSO AP Tax Specialists). Subtask of (UAF-92)
-- =======================================================================================================================
--The below SQL is to repurpose 4 of the KFS 3.0 values to use as KFS 6 values and delete the extra 5th detail value that came over from 3.0
DELETE FROM KULOWNER.KRIM_RSP_ATTR_DATA_T WHERE ATTR_DATA_ID IN ('165178');
UPDATE KULOWNER.KRIM_RSP_ATTR_DATA_T SET ATTR_VAL = 'KFS' WHERE ATTR_DATA_ID IN ('158');
--The responsibility is actually active in 6.0, but the responsibility role connection in the KRIM_ROLE_RSP_T table is inactive. The below SQL is to make that active:
UPDATE KULOWNER.KRIM_ROLE_RSP_T SET ACTV_IND = 'Y' WHERE ROLE_ID='10403';
UPDATE KULOWNER.KRIM_RSP_T SET ACTV_IND = 'Y' WHERE RSP_ID='49';
UPDATE KULOWNER.KRIM_RSP_ATTR_DATA_T SET ATTR_VAL = 'true' WHERE ATTR_DATA_ID IN ('159');
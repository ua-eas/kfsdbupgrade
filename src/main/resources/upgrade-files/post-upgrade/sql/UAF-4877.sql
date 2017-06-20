-- =======================================================================================================================
-- UAF-4877 : Deactivate UA FSO Property Management from Role 1305 for Cash Receipt Document
-- =======================================================================================================================

UPDATE KRIM_ROLE_MBR_T SET ACTV_TO_DT=TO_DATE(CURRENT_DATE, 'yyyy/mm/dd hh24:mi:ss') WHERE ROLE_MBR_ID='89713';
UPDATE KRIM_ROLE_MBR_T SET ACTV_TO_DT=TO_DATE(CURRENT_DATE, 'yyyy/mm/dd hh24:mi:ss') WHERE ROLE_MBR_ID='89714';
UPDATE KRIM_ROLE_MBR_T SET ACTV_TO_DT=TO_DATE(CURRENT_DATE, 'yyyy/mm/dd hh24:mi:ss') WHERE ROLE_MBR_ID='89724';
UPDATE KRIM_ROLE_MBR_T SET ACTV_TO_DT=TO_DATE(CURRENT_DATE, 'yyyy/mm/dd hh24:mi:ss') WHERE ROLE_MBR_ID='89725';
UPDATE KRIM_ROLE_MBR_T SET ACTV_TO_DT=TO_DATE(CURRENT_DATE, 'yyyy/mm/dd hh24:mi:ss') WHERE ROLE_MBR_ID='89746';

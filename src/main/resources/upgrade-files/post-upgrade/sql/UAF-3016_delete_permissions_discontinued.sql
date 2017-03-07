-- =============================================================================================================
-- (UAF-3016) : Delete unused permissions from discontinued mod MOD-GNRL-05. Subtask of (UAF-92)
-- =============================================================================================================
DELETE FROM KULOWNER.KRIM_ROLE_PERM_T WHERE PERM_ID IN ('11534','10562','10109','10488','10489','10490','10538','10539','10541','10542','10544','10545','10546','10547','10548','10551','10250','10251','10633');
DELETE FROM KULOWNER.KRIM_PERM_ATTR_DATA_T WHERE PERM_ID IN ('11534','10562','10109','10488','10489','10490','10538','10539','10541','10542','10544','10545','10546','10547','10548','10551','10250','10251','10633');
DELETE FROM KULOWNER.KRIM_PERM_T WHERE PERM_ID IN ('11534','10562','10109','10488','10489','10490','10538','10539','10541','10542','10544','10545','10546','10547','10548','10551','10250','10251','10633');
-- The following is to remove all roles from the permission 362 (base permission) except role 10448 - Temp Batch Runner, which will be inactive in Production Environment:
UPDATE KULOWNER.KRIM_ROLE_PERM_T SET ACTV_IND = 'N' WHERE PERM_ID = '362' AND ROLE_ID IN ('10441','10111','10385');


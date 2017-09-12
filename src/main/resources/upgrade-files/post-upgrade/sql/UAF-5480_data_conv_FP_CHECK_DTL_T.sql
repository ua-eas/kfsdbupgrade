-- =============================================================================================================
-- UAF-5480 : Copying a 3.0 Cash Receipt Bursar Rep unable to approve document due to error message
-- Description of issue: KFS 5.0 delivered 4.1.1_5.0/db/01_structure/fp-module-structure-updates.xml  with 
-- 		change set ID = KFS_50_FP_DEPOSIT_HDR_T to convert data for DB FP_CHECK_DTL_T replace FDOC_COLUMN_TYP_CD
-- 		column by CSHR_STAT_CD. It uses the existing value in that field to set the new CSHR_STAT_CD value *if* 
-- 		the FDOC_TYP_CD on the record is “CR” or “CM”. Existing value of FDOC_COLUMN_TYP_CD may have 'C' for checks
--		or 'R' for confirmed checks. However, UA runs 3.0 in PROD which does not have 'C' checks value. Only 'R' exists
-- 		to represent checks/confirmed checks.
-- Solution: UA developed data conversion script to accomodate this DB change. This DB conversion script include two steps,
--		1. Convert 'C' check type for all entries in FP_CHECK_DTL_T regardless CR document routing status
--		2. Creating new 'R' confirmed check entries in FP_CHECK_DTL_T by duplicating 'C' check entry from the same CR.
--			Creating new 'R' confirmed checks for CR if it's Final or Processed, 'A' or 'V' in FS_DOC_HEADER_T
-- =============================================================================================================
-- Step 1
UPDATE FP_CHECK_DTL_T T
SET CSHR_STAT_CD    ='C'
WHERE T.FDOC_TYP_CD = 'CR'
AND T.CSHR_STAT_CD  = 'R';

-- Step 2
INSERT
INTO FP_CHECK_DTL_T
  (
    FDOC_NBR,
    FDOC_TYP_CD,
    FDOC_LINE_NBR,
    OBJ_ID,
    VER_NBR,
    FDOC_CHCK_NBR,
    FDOC_CHCK_DT,
    FDOC_CHCK_DESC,
    FDOC_CHCK_AMT,
    FDOC_DPST_LN_NBR,
    CSHR_STAT_CD
  )
SELECT T1.FDOC_NBR,
  T1.FDOC_TYP_CD,
  T1.FDOC_LINE_NBR,
  SYS_GUID(),
  T1.VER_NBR,
  T1.FDOC_CHCK_NBR,
  T1.FDOC_CHCK_DT,
  T1.FDOC_CHCK_DESC,
  T1.FDOC_CHCK_AMT,
  T1.FDOC_DPST_LN_NBR,
  'R'
FROM FP_CHECK_DTL_T T1 ,
  FS_DOC_HEADER_T T2
WHERE T1.FDOC_NBR      =T2.FDOC_NBR
AND t2.fdoc_status_cd IN ('A','V');
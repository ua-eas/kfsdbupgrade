--------------------------------------------------------------------------------
-- UAF-3965 : Delete Redundant 3.0 Parameter ENABLE_SALES_TAX_EXEMPT_IND from 6.0. Subtask of (UAF-92)
--------------------------------------------------------------------------------
DELETE FROM KRCR_PARM_T WHERE NMSPC_CD='KFS-FP' AND CMPNT_CD='ProcurementCard' AND APPL_ID='KFS' AND PARM_NM='ENABLE_SALES_TAX_EXEMPT_IND';
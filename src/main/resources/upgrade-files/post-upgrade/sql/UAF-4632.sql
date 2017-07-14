--------------------------------------------------------------------------------
-- UAF-4632 : Delete Redundant 6.0 Parameter POSTAL_CODE_DIGITS_PASSED_TO_SALES_TAX_REGION_SERVICE. Subtask of (UAF-92)
--------------------------------------------------------------------------------
DELETE FROM KRCR_PARM_T WHERE NMSPC_CD='KFS-FP' AND CMPNT_CD='SalesTax' AND APPL_ID='KUALI' AND PARM_NM='POSTAL_CODE_DIGITS_PASSED_TO_SALES_TAX_REGION_SERVICE';

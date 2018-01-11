-- ============================================================================
-- UAF-6462 : Update parameters from 7.0 Parameters Analysis. Subtask of (UAF-92)
-- ============================================================================
UPDATE KRCR_PARM_T SET VAL = '9001' WHERE PARM_NM = 'DEFAULT_PROCESSING_ORG' AND NMSPC_CD = 'KFS-AR' AND CMPNT_CD = 'CashControl';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'RESEARCH_ADMIN_AUTO_CREATE_ACCOUNT_WORKFLOW_ACTION' AND NMSPC_CD = 'KFS-COA' AND CMPNT_CD = 'Account'; 
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'IMMEDIATE_EXTRACT_NOTIFICATION_TO_EMAIL_ADDRESSES' AND NMSPC_CD = 'KFS-FP' AND CMPNT_CD = 'DisbursementVoucher';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'INVALID_OBJECT_SUB_TYPES_BY_SUB_FUND_GROUP' AND NMSPC_CD = 'KFS-FP' AND CMPNT_CD = 'DisbursementVoucher';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'VALID_OBJECT_SUB_TYPES_BY_SUB_FUND_GROUP' AND NMSPC_CD = 'KFS-FP' AND CMPNT_CD = 'DisbursementVoucher';
UPDATE KRCR_PARM_T SET VAL = 'uaccess_financials_no_reply@list.arizona.edu' WHERE PARM_NM = 'FROM_EMAIL_ADDRESS' AND NMSPC_CD = 'KFS-GL' AND CMPNT_CD = 'Batch';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'OBJECT_TYPE_BYPASS_ORIGINATIONS' AND NMSPC_CD = 'KFS-GL' AND CMPNT_CD = 'ScrubberStep';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'TRANSACTION_DATE_BYPASS_ORIGINATIONS' AND NMSPC_CD = 'KFS-GL' AND CMPNT_CD = 'ScrubberStep';                                                  
UPDATE KRCR_PARM_T SET VAL = 'uaccess_financials_no_reply@list.arizona.edu' WHERE PARM_NM = 'FROM_EMAIL_ADDRESS' AND NMSPC_CD = 'KFS-PDP' AND CMPNT_CD = 'Batch';
UPDATE KRCR_PARM_T SET VAL = '' WHERE PARM_NM = 'RESEARCH_PARTICIPANT_UPLOAD_CUSTOMER_PROFILE' AND NMSPC_CD = 'KFS-PDP' AND CMPNT_CD = 'PaymentDetail';
UPDATE KRCR_PARM_T SET VAL = 'uaccess_financials_no_reply@list.arizona.edu' WHERE PARM_NM = 'DEFAULT_FROM_EMAIL_ADDRESS' AND NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'All';
UPDATE KRCR_PARM_T SET VAL = 'FSO_KFS_Reports_Financial_Management@fso.arizona.edu' WHERE PARM_NM = 'DEFAULT_TO_EMAIL_ADDRESS' AND NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'All';
                                                                                                                    
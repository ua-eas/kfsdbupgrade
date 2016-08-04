-- =======================================================================================================================
-- (UAF-2745) : Update Disbursement Voucher Payment Reason(FP) Table. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.FP_DV_PMT_REAS_T SET DV_PMT_REAS_DESC = 'Payment for Freight and Utilities such as telephone service, internet service, etc. Use object level codes UTIL, COMM, COS and MISC for utility and freight object codes.' WHERE DV_PMT_REAS_CD = 'K';

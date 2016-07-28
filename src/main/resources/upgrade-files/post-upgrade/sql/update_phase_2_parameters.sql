-- ======================================================================================================
-- Update Phase 2 Parameters in Upgrade Scripts (UAF-2043) subtask of (UAF-92)
-- set define off;   -- Disables definintion of substitution variables to allow 'Ampersand character' within strings and comments this maybe only needed in SQLDeveloper tho
-- ======================================================================================================
update KRCR_PARM_T set VAL = 'A=V;B=E,V;E=E,V;F=V;H=E,V;I=V;K=V;M=E;N=E,V;P=E,V;T=E,V;X=E,V;Y=E,V;V=E'  where PARM_NM = 'VALID_PAYEE_TYPES_BY_PAYMENT_REASON';
set define off;  
update KRCR_PARM_T set VAL = 'Click on the category link to connect to the Federal, State of Arizona and Foreign web site listing their official per diem rates. Once you have determined the correct M & IE rate, please enter the amount in the Per Diem Rate field. You will have to manually calculate the per diem amount and enter it in the Per Diem Actual Amount field. The Travel Office will verify the per diem amount from the lodging receipts included with the supporting documentation.'  where PARM_NM = 'TRAVEL_PER_DIEM_LINK_PAGE_MESSAGE';
update KRCR_PARM_T set VAL = '14'  where PARM_NM = 'AUTO_APPROVE_NUMBER_OF_DAYS';
update KRCR_PARM_T set VAL = 'TXEX;LIBR'  where PARM_NM = 'TAXABLE_OBJECT_LEVELS_FOR_NON_TAXABLE_STATES';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'TAXABLE_OBJECT_CONSOLIDATIONS_FOR_NON_TAXABLE_STATES';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'TAXABLE_OBJECT_CONSOLIDATIONS_FOR_TAXABLE_STATES';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'AUTO_CLOSE_PO_NUMBER_UPPER';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'UNORDERED_ITEM_DEFAULT_COMMODITY_CODE';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'AUTO_CLOSE_PO_NUMBER_LOWER';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'RESEARCH_ADMIN_BA_DOCUMENT_ROUTE_ACTION';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'PAYEE_TYPE_NAME';
update KRCR_PARM_T set VAL = 'N'  where PARM_NM = 'AUTO_ADD_COMMODITY_CODES_TO_VENDOR_IND';
update KRCR_PARM_T set VAL = 'N'  where PARM_NM = 'ENABLE_DEFAULT_VENDOR_COMMODITY_CODE_IND';
update KRCR_PARM_T set VAL = 'kfs_pcard_errors@list.arizona.edu'  where PARM_NM = 'PCARD_BATCH_SUMMARY_TO_EMAIL_ADDRESSES';
update KRCR_PARM_T set VAL = 'N'  where PARM_NM = 'W8_DATA_REQUIRED_IND';
update KRCR_PARM_T set VAL = 'N'  where PARM_NM = 'W9_SIGNED_DATE_REQUIRED_IND';
update KRCR_PARM_T set VAL = 'AGUV'  where PARM_NM = 'SUPPRESS_REJECT_REASON_CODES_ON_EIRT_APPROVAL';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'IMMEDIATE_EXTRACT_NOTIFICATION_FROM_EMAIL_ADDRESS';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'IMMEDIATE_EXTRACT_NOTIFICATION_TO_EMAIL_ADDRESS';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'B2B_CLASSIFICATION_FOR_COMMODITY_CODE';
update KRCR_PARM_T set VAL = 'Y' where PARM_NM = 'SENSITIVE_DATA_NOTE_IND';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'PAYMENT_REASON_CODE_ROYALTIES';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'PAYMENT_REASON_CODE_RENTAL_PAYMENT';
update KRCR_PARM_T set VAL = '' where PARM_NM = 'PAYMENT_REASON_CODE_TRAVEL_HONORARIUM';
update KRCR_PARM_T set VAL = 'N' where PARM_NM = 'ADD_PAYEE_TAX_REVIEW_FLAG_TO_DV_TITLE_IND';
update KRCR_PARM_T set VAL = 'N' where PARM_NM = 'ADD_PAYMENT_REASON_TAX_REVIEW_FLAG_TO_DV_TITLE_IND';
update KRCR_PARM_T set VAL = 'B2B;STAN;BTCH' where PARM_NM = 'NOTIFY_REQUISITION_SOURCES';










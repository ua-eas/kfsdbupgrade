-- ======================================================================================================
-- (UAF-2044) Add back parameters deleted from database, subtask of (UAF-92)
-- ======================================================================================================
insert into KULOWNER.KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) values ('KFS','KFS-FP','DisbursementVoucher','INVALID_OBJECT_SUB_TYPES_BY_SUB_FUND_GROUP',1,'VALID','','Defines an invalid relationship between the Sub-Fund Group and the Object Sub-Type(s) on the Disbursement Voucher document. Format of list is sub fund group 1=object sub type 1, object sub type 2;sub fund group 2=object sub type 3,object sub type 4,object sub type 5.','A',SYS_GUID());
insert into KULOWNER.KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) values ('KFS','KFS-FP','DisbursementVoucher','VALID_OBJECT_SUB_TYPES_BY_SUB_FUND_GROUP',1,'VALID','LOANFD=','Defines a valid relationship between the Sub-Fund Group and the Object Sub-Type(s) on the Disbursement Voucher document. Format of list is sub fund group 1=object sub type 1, object sub type 2;sub fund group 2=object sub type 3,object sub type 4,object sub type 5.','A',SYS_GUID());
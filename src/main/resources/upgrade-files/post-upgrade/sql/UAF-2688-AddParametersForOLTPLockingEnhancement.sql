-- ============================================================================
--  Add parameters for OLTP locking enhancement (UAF-2688) subtask of (UAF-92)
-- ============================================================================
insert into KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) values ('KUALI','KR-NS','All','OLTP_LOCKOUT_DEFAULT_MESSAGE',1,'CONFG','The module you are attempting to access has been locked for maintenance.','Default message to display when a module is locked','A',SYS_GUID());
insert into KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) values ('KFS','KR-NS','All','OLTP_LOCKOUT_DEFAULT_MESSAGE',1,'CONFG','This module is currently locked for maintenance. Please try to access it again later','This parameter is to set the default message for the various modules during the lockout time ','A',SYS_GUID());

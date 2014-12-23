alter table KRLC_CNTRY_T add ALT_POSTAL_CNTRY_CD VARCHAR2(3);
update krim_rsp_t set ver_nbr = 1 where ver_nbr = 0;
insert into krim_grp_t (grp_id, obj_id, ver_nbr, grp_nm, nmspc_cd, kim_typ_id, actv_ind, last_updt_dt) values (krim_grp_id_s.nextVal, sys_guid(), 1, 'WorkflowAdmin', 'KR-WKFLW', 'KFS68', 'Y', sysdate);
insert into krsb_qrtz_locks (lock_name) values ('STATE_ACCESS');
insert into krsb_qrtz_locks (lock_name) values ('TRIGGER_ACCESS');
alter table pur_po_t modify PO_DOC_CRTE_DT null;
update krcr_parm_t set val = 'DV=0002;ND=0001;AD=0001;CMD=0001;PREQ=0002;CM=0002;CTRL=0001;CCR=0006;DI=0001' where nmspc_cd = 'KFS-FP' and parm_nm ='DEFAULT_BANK_BY_DOCUMENT_TYPE';
update krcr_parm_t set val = 'DV=0002;ND=0001;AD=0001;CMD=0001;PREQ=0002;CM=0002;CTRL=0001;CCR=0006;TA=TEST;TR=TEST;RELO=TEST;ENT=TEST' where nmspc_cd = 'KFS-SYS' and parm_nm ='DEFAULT_BANK_BY_DOCUMENT_TYPE';
update krcr_parm_t set val = 'CH;IC;IN;TI' where nmspc_cd = 'KFS-GL' and parm_nm ='INCOME_OBJECT_TYPE';
update krcr_parm_t set val = 'EE;ES;EX;TE' where nmspc_cd = 'KFS-GL' and parm_nm ='EXPENSE_OBJECT_TYPE';
UPDATE KRCR_PARM_T SET VAL = 'Y' WHERE PARM_NM = 'PROCUREMENT_CARD_ACCOUNTING_DEFAULT_IND'
UPDATE KRCR_PARM_T SET VAL = 'Y' WHERE PARM_NM = 'PROCUREMENT_CARD_HOLDER_DEFAULT_IND'



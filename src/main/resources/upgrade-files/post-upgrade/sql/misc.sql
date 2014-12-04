alter table KRLC_CNTRY_T add ALT_POSTAL_CNTRY_CD VARCHAR2(3);
update krim_rsp_t set ver_nbr = 1 where ver_nbr = 0;
insert into krim_grp_t (grp_id, obj_id, ver_nbr, grp_nm, nmspc_cd, kim_typ_id, actv_ind, last_updt_dt) values (krim_grp_id_s.nextVal, sys_guid(), 1, 'WorkflowAdmin', 'KR-WKFLW', 'KFS68', 'Y', sysdate);
insert into krsb_qrtz_locks (lock_name) values ('STATE_ACCESS');
insert into krsb_qrtz_locks (lock_name) values ('TRIGGER_ACCESS');
alter table pur_po_t modify PO_DOC_CRTE_DT null;
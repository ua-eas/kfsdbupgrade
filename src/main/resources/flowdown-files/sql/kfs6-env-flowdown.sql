-- ====================================================================================================================
-- 0)Pre-requisites  Clean all the triggers and quartz tables. 
--                   Clear all the roles for any existing kfs-test-sec* or kfs-test-sys* users
--                   Add kfs-test-sec* and sys9 and sys10 to the Basic Role: Financial System User
--                   Make sure all users have an options record, defaulted to no email notifications
--                   Action List Notification Disabling - set to "no"
-- ====================================================================================================================

-- Clear the quartz and KSB tables 
truncate table kulowner.QRTZ_FIRED_TRIGGERS;
truncate table kulowner.QRTZ_JOB_LISTENERS;
truncate table kulowner.QRTZ_SIMPLE_TRIGGERS;
truncate table kulowner.QRTZ_SCHEDULER_STATE;
truncate table kulowner.KRSB_MSG_PYLD_T;
truncate table kulowner.KRSB_MSG_QUE_T;

--attempting to truncate KRSB_SVC_DSCRPTR_T throws an error, so directly delete and commit
delete from kulowner.KRSB_SVC_DSCRPTR_T;
commit;

delete from QRTZ_BLOB_TRIGGERS;
delete from QRTZ_CRON_TRIGGERS;
delete from QRTZ_SIMPLE_TRIGGERS;
delete from QRTZ_TRIGGER_LISTENERS;

delete from kulowner.QRTZ_TRIGGERS;
delete from kulowner.QRTZ_JOB_DETAILS;


-- Clear all the roles for any existing kfs-test-sec* or kfs-test-sys8 and sys10 users
-- Other sys users should have their roles left intact, and are not included in this delete
delete from krim_role_mbr_t 
where mbr_typ_cd = 'P' and mbr_id in 
	( select prncpl_id from kulowner.krim_entity_cache_t 
      where prncpl_nm like 'kfs-test-sec%' 
            or prncpl_nm in ('kfs-test-sys10', 'kfs-test-sys8')
	) 
;

-- re-add all kfs-test-sec* and kfs-test-sys8 and sys10 to role basic role Financial System User - 54 
-- Only sys8 and sys10 users specifically need role 54; other sys users should not have it
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd, actv_frm_dt, actv_to_dt ) 
  select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '54', prncpl_id, 'P', null, null 
  from kulowner.krim_entity_cache_t 
  where prncpl_nm like 'kfs-test-sec%' 
        or prncpl_nm in ('kfs-test-sys10', 'kfs-test-sys8') 
;

-- Make sure all users have an options record, defaulted to no email notifications
insert into krew_usr_optn_t ( PRNCPL_ID, PRSN_OPTN_ID, VAL, VER_NBR ) 
  select distinct a.mbr_id, 'EMAIL_NOTIFICATION', 'no', 1 
  from krim_role_mbr_t a
  where not exists 
   (select * from krew_usr_optn_t b where b.prncpl_id = a.mbr_id)
; 

-- Action List Notification Disabling - set to "no"
update KULOWNER.krew_usr_optn_t set val = 'no' where prsn_optn_id  
  in ('EMAIL_NOTIFICATION', 'EMAIL_NOTIFY_PRIMARY', 'EMAIL_NOTIFY_SECONDARY')
;


-- ====================================================================================================================
-- 1) Add "UA Super User Group" members: kfs-test-sec25, kfs-test-sec29 
-- ====================================================================================================================


-- kfs-test-sec25 (#T000000000000005416) member of "UA Super User Group" G#1053323 
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1053323', 'T000000000000005416', 'P')
; 
 

-- add kfs-test-sec29 ( #T000000000000005461) to UA Super User group (#1053323)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1053323', 'T000000000000005461', 'P' from dual
; 


-- ====================================================================================================================
-- 1b) Batch Modifier Role: kfs-test-sec25
-- ====================================================================================================================

-- add kfs-test-sec25 #T000000000000005416 to "Batch Job Modifier" role #82
-- insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd ) 
-- values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '82', 'T000000000000005416', 'P')
-- ;


-- ====================================================================================================================
-- 2) Make sure all groups where test ID's are listed are active: All kfs-test-sec*, kfs-test-eds*, and kfs-test-sys" 
--    and only kfs-test-sys10 and 8
-- ====================================================================================================================

--Q: Why only sys10 and sys8, what about the other test-sys?

update krim_grp_t set actv_ind = 'Y' 
where grp_id in 
    (  select grp_id from krim_grp_mbr_t 
       where mbr_typ_cd = 'P' and mbr_id in (
                    select prncpl_id 
                    from krim_entity_cache_t 
                    where prncpl_nm like 'kfs-test-sec%' 
					or prncpl_nm like 'kfs-test-eds%'
                    or prncpl_nm like 'kfs-test-sys%'
                    or prncpl_nm in ('kfs-test-sys10', 'kfs-test-sys8')
                )
    )
;
  



-- ====================================================================================================================
-- 3)OBFUSCATE SENSITIVE DATA: Set each vendor to have a different randomly assigned number for Tax ID and encrypt it.
--   For the Tax Id Types that are set to NONE, it leaves it as is (so it allows blank values)
-- !!! Note: KULOWNER needs permission to run the DBMS_CRYPTO package for ENCRYPTING the dummy numbers
-- ====================================================================================================================
-- Create/Update the DES Encrypt function in the DB
CREATE OR REPLACE
FUNCTION          DES_ENCRYPT (p_plainText VARCHAR2, encryption_key RAW) RETURN VARCHAR2
IS
    encryptedValue      VARCHAR2(255);
    encryption_type     PLS_INTEGER := dbms_crypto.ENCRYPT_DES + DBMS_CRYPTO.CHAIN_ECB + DBMS_CRYPTO.PAD_PKCS5;
    
BEGIN
	encryptedValue:= dbms_crypto.ENCRYPT(
		src => UTL_RAW.cast_to_raw (p_plainText),
        typ => encryption_type,
        key => encryption_key
  );
  RETURN UTL_I18N.RAW_TO_CHAR( UTL_ENCODE.base64_encode(encryptedValue), 'utf8');
END;
/

-- NEW: 9000000000000 + ROW# -  for account numbers 

UPDATE fp_dv_ach_t set dv_payee_acct_nbr = DES_ENCRYPT( ROWNUM + 9000000000000 , '${ENCRYPTION_KEY}');
UPDATE KULOWNER.fs_pmt_src_wire_trnfr_t set payee_acct_nbr = DES_ENCRYPT( ROWNUM + 9000000000000 , '${ENCRYPTION_KEY}');
UPDATE pdp_ach_acct_nbr_t set ach_bnk_acct_nbr = DES_ENCRYPT( ROWNUM + 9000000000000 , '${ENCRYPTION_KEY}');
UPDATE pdp_payee_ach_acct_t set bnk_acct_nbr = DES_ENCRYPT( ROWNUM + 9000000000000 , '${ENCRYPTION_KEY}');

-- NEW: 900000000 + ROW# - for tax id numbers that are not of type NONE
UPDATE pur_vndr_hdr_t set vndr_us_tax_nbr = DES_ENCRYPT( ROWNUM + 900000000 , '${ENCRYPTION_KEY}') where vndr_tax_typ_cd != 'NONE';
UPDATE pur_vndr_tax_chg_t set vndr_prev_tax_nbr = DES_ENCRYPT( ROWNUM + 899999999 , '${ENCRYPTION_KEY}') where vndr_prev_tax_typ_cd != 'NONE';
UPDATE tax_payee_t set hdr_vndr_tax_nbr = DES_ENCRYPT( ROWNUM + 900000000 , '${ENCRYPTION_KEY}');

--OLD:
-- update fp_dv_ach_t set dv_payee_acct_nbr = 'r+181z6uNTJrgbJPn0ljGA==';
-- update fs_pmt_src_wire_trnfr_t set payee_acct_nbr = 'r+181z6uNTJrgbJPn0ljGA==';
-- update pdp_ach_acct_nbr_t set ach_bnk_acct_nbr = 'r+181z6uNTJrgbJPn0ljGA==';
-- update pdp_payee_ach_acct_t set bnk_acct_nbr = 'r+181z6uNTJrgbJPn0ljGA==';
-- update pur_vndr_hdr_t set vndr_us_tax_nbr = 'r+181z6uNTIc3lalnjPKpA==';
-- update pur_vndr_tax_chg_t set vndr_prev_tax_nbr = 'r+181z6uNTIc3lalnjPKpA==';
-- update tax_payee_t set hdr_vndr_tax_nbr = 'r+181z6uNTIc3lalnjPKpA==';

-- ====================================================================================================================
-- 4) Add test ID kfs-test-sec9 (mbr_id# T000000000000005400) to ALL groups starting with UA PCRD.
-- ====================================================================================================================

-- Make kfs-test-sec9  (#T000000000000005400) a member of ALL UA PCRD groups
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
  select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), grp_id, 'T000000000000005400', 'P' 
  from krim_grp_t
  where grp_nm like 'UA PCRD%'
; 




-- ====================================================================================================================
-- 5) Activate kfs-test-sec40 for the Contract Manager 90 in the Contract Manager table
-- ====================================================================================================================

-- activate kfs-test-sec40 to Contract Manager Table with one-million-dollar limit
-- Values: Contract Manager Code, Contract Manager Name, Published Phone Number, Published Fax Number, Contract Manager Delegation Dollar Limit, Active Indicator 
--              90               kfs-test-sec40             555-555-5555          556-555-5555              1,000,000                             Yes 

update pur_contr_mgr_t set ACTV_IND='Y'
where contr_mgr_cd='90' and contr_mgr_nm='kfs-test-sec40' and contr_mgr_dlgn_dlr_lmt=1000000.00
;



-- ====================================================================================================================
-- 6)  Add kfs-test-sec37 to the group UA FSO Accounts Payable Managers
-- ====================================================================================================================

-- Add kfs-test-sec37 (T000000000000005469) to UA FSO Accounts Payable Managers Group (#1051033)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1051033', 'T000000000000005469', 'P')
;



-- ====================================================================================================================
-- 7)  Update KFS-SYS BI_REPORT_URL Parameter so that the value is https://eiasupetlbi.uaccess.arizona.edu/analytics/ 
--     (per MBI-5604) so that BI SUP is accessible.
-- ====================================================================================================================

-- Set BI_REPORT_URL to https://eiasupbi.uaccess.arizona.edu/analytics/
update kulowner.krcr_parm_t set val = 'https://eiasupetlbi.uaccess.arizona.edu/analytics/'
where nmspc_cd = 'KFS-SYS' and cmpnt_cd = 'All' and parm_nm = 'BI_REPORT_URL'
;



-- ====================================================================================================================
-- 8)Allow kfs-test-sec25 to modify batch jobs and view batch files  
-- ROLE: KFS-SYS TEMP Batch Runner,(role#10448)  GROUP: UA KFS Business Analysts (grp#1052641)
-- ====================================================================================================================

-- Make kfs-test-sec25 (#T000000000000005416) member of UA KFS Business Analysts (grp#1052641) 
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1052641', 'T000000000000005416', 'P')
;


-- ====================================================================================================================
-- 9) Add kfs-test-sec13 and kfs-test-sec29 to UA FSO Accounts Payable Check Recon Group (#1051594). 
-- ====================================================================================================================

-- Add kfs-test-sec13 (T000000000000005404) to UA FSO Accounts Payable Check Recon Group (#1051594)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1051594', 'T000000000000005404', 'P')
;

-- add kfs-test-sec29 ( #T000000000000005461) to UA FSO Accounts Payable Check Recon group (#1051594)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1051594', 'T000000000000005461', 'P')
; 




-- ====================================================================================================================
-- 10) UA FSO AP Specialists EIRT Team (#1050641) group has test id kfs-test-sec41 (#T000000000000005473), 
--     and add perm to open an EIRT document to that group.
-- ====================================================================================================================

-- Add kfs-test-sec41 (T000000000000005473) to UA FSO AP Specialists EIRT Team Group (#1050641)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050641', 'T000000000000005473', 'P')
;

-- make sure role 10382 - UA FSO Accounts Payable EIRT Specialist has both necessary EIRT permissions 
insert into krim_role_perm_t ( role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind ) 
values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10382', '291', 'Y')
;




-- ====================================================================================================================
-- 11) Add userid kfs-test-sec40 and  kfs-test-sec66 to the UA PACS Buyers Group GRP#1050058
-- ====================================================================================================================
-- Add kfs-test-sec40 (T000000000000005472) to UA PACS Buyers Group 
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050058', 'T000000000000005472', 'P')
;

-- kfs-test-sec66 #T000000000000005504 to UA PACS Buyers Group 
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050058', 'T000000000000005504', 'P')
; 



-- ====================================================================================================================
-- 12a) Give 'UA KFS Developers' ROLE#10385 permissions for Batch Lookup and Modify Batch Job
-- ====================================================================================================================

-- Give to the "UA KFS Developers" role the permission to access the batch file lookup 
insert into krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind)
values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), 
  (select role_id from krim_role_t where role_nm='UA KFS Developers'), 
  (select perm_id from krim_perm_t where nmspc_cd='KFS-SYS' and desc_txt='Allow users to access the Batch File lookup.'),
  'Y')
;

-- Give to the "UA KFS Developers" role the persmission to modify batch job
-- Q: Is it OK if the namespace is just KFS-SYS? Or should it be KFS*?
insert into krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind)
values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), 
  (select role_id from krim_role_t where role_nm='UA KFS Developers'),
  (select perm_id from krim_perm_t where nmspc_cd='KFS-TAX' and nm='Modify Batch Job'),
  'Y')
;



-- ====================================================================================================================
-- 12b) Give UA KFS Business Analysts' Group the TEMP Batch Runner Role so they can have Batch Modify/View Permissions
-- ====================================================================================================================

-- "UA KFS Business Analysts" group has "TEMP Batch Runner" role
insert into krim_role_mbr_t (role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd)
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), 
  (select role_id from krim_role_t where role_nm='TEMP Batch Runner'),
  (select grp_id from krim_grp_t where grp_nm='UA KFS Business Analysts'), 
  'G')
;



-- ====================================================================================================================
-- 13) Add UserID kfs-test-sec25 to the UA FSO Parameter Managers Group to approve parm changes
-- ====================================================================================================================

--kfs-test-sec25 #T000000000000005416 in UA FSO Parameter Managers Group# 1051765
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1051765', 'T000000000000005416', 'P')
;



-- ====================================================================================================================
-- 13b) Add UserID kfs-test-sec25 to the UA FSO Financial Management Managers Group to approve parm changes
-- ====================================================================================================================

--OLD: kfs-test-sec25 #T000000000000005416 member of KFS-FP:UA FSO Financial Management Managers G#1050065
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050065', 'T000000000000005416', 'P')
; 




-- ====================================================================================================================
-- 14a) Add kfs-test-sec79 as a member of the Contract Manager Role (R#25) with qualifier 80 
-- ====================================================================================================================

--  kfs-test-sec79 #T000000000000005517 has Contract Manager Role (R#25)
insert into krim_role_mbr_t (role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd)
values  (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '25', 'T000000000000005517', 'P')
;

--kfs-test-sec79 #T000000000000005517 Add qualifier '80' to his Contract Manager Role membership 
insert into krim_role_mbr_attr_data_t (ATTR_DATA_ID, OBJ_ID, VER_NBR, ROLE_MBR_ID, ATTR_VAL, KIM_ATTR_DEFN_ID, KIM_TYP_ID)
values ( KRIM_ATTR_DATA_ID_S.NEXTVAL, SYS_GUID(), 1,
  (select ROLE_MBR_ID from krim_role_mbr_t where mbr_id='T000000000000005517' and role_id='25'),
  '80', '32', '34' ) 
;


-- ====================================================================================================================
-- 14b) Add kfs-test-sec79 to PACS Manager Group and the PACS Buyers Group
-- ====================================================================================================================

--  kfs-test-sec79 #T000000000000005517 to UA PACS Managers Group G#1050086
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050086', 'T000000000000005517', 'P')
; 

--  kfs-test-sec79 #T000000000000005517 to UA PACS Buyers Group G#1050058
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050058', 'T000000000000005517', 'P')
; 

-- ====================================================================================================================
-- 14c) Add  kfs-test-sec79 to the Contract Manager table with the following values: 
--      Contract Manager Code Contract Manager Name Published Phone Number Published Fax Number Contract Manager Delegation Dollar Limit Active Indicator
--              80                kfs-test-sec79         555-555-6666            556-555-6666                       0.00                    Yes
-- ====================================================================================================================

insert into pur_contr_mgr_t (CONTR_MGR_CD, OBJ_ID, VER_NBR, CONTR_MGR_NM, CONTR_MGR_PHN_NBR, CONTR_MGR_FAX_NBR, CONTR_MGR_DLGN_DLR_LMT, ACTV_IND)
values ('80', SYS_GUID(), 1, 'kfs-test-sec79', '555-555-6666', '556-555-6666', 0.0, 'Y')
;



-- ====================================================================================================================
-- 15) Replace all listserv email addresses in parameters with KFSBSATEAM@LIST.ARIZONA.EDU
-- ====================================================================================================================

-- PDP Customer Profile Email Addresses - kfsbsateam@list.arizona.edu
update kulowner.pdp_cust_prfl_t set cust_prcs_email_addr = 'kfsbsateam@list.arizona.edu' where cust_prcs_email_addr is not null or cust_prcs_email_addr != '';
update kulowner.pdp_cust_prfl_t set cust_pmt_thrshld_email_addr = 'kfsbsateam@list.arizona.edu' where cust_pmt_thrshld_email_addr is not null or cust_pmt_thrshld_email_addr != '';
update kulowner.pdp_cust_prfl_t set cust_file_thrshld_email_addr = 'kfsbsateam@list.arizona.edu' where cust_file_thrshld_email_addr is not null or cust_file_thrshld_email_addr != '';
update kulowner.pdp_cust_prfl_t set adv_rtrn_email_addr = 'kfsbsateam@list.arizona.edu' where adv_rtrn_email_addr is not null or adv_rtrn_email_addr != '';

-- Parameter Values containing emails - kfsbsateam@list.arizona.edu
update kulowner.krcr_parm_t set val = 'N' where nmspc_cd = 'KR-WKFLW' and parm_typ_cd = 'ActionList' and parm_nm = 'SEND_EMAIL_NOTIFICATION_IND'; 
update kulowner.krcr_parm_t set val = 'kfsbsateam@list.arizona.edu' where val like '%@%' and parm_nm not in ('PDF_STATUS_INQUIRY_URL', 'FROM_ADDRESS');


-- ====================================================================================================================
-- 16) Activate STG TEMP Batch Runner Role (#10448) 
--     Add permissions to the role: View Batch Jobs (#362), Modify Batch job (#11774 ), View Batch Job (#10491)
-- ====================================================================================================================
-- Activate Temp Batch Runner role - 
update kulowner.krim_role_t set ACTV_IND = 'Y'
   where role_nm='TEMP Batch Runner';

-- Add View Batch Jobs (#362) permission to TEMP Batch Runner (#10448) - 
INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10448', '362','Y');

-- Add Modify Batch Job (#11774) permission to TEMP Batch Runner (#10448) - 
INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10448','11774','Y');

-- Add View Batch Job (#10491) permission to TEMP Batch Runner (#10448) - 
INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10448','10491','Y');


-- ====================================================================================================================
-- 17) Add kfs-test-sec8 to groups: UA Budget Office Managers G#1050029 and  UA Budget Office Shell Code Maintainers G#1053223
-- ====================================================================================================================

-- kfs-test-sec8 #T000000000000005399 to UA Budget Office Shell Code Maintainers G#1053223
insert into krim_grp_mbr_t (grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd)
values ( KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), 
  ( select grp_id from krim_grp_t where grp_nm='UA Budget Office Shell Code Maintainers'), 
  'T000000000000005399', 'P')
;

-- kfs-test-sec8 #T000000000000005399 to UA Budget Office Managers G#1050029
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050029', 'T000000000000005399', 'P')
; 



-- ====================================================================================================================
-- 18) Add kfs-test-sec37 UA FSO Disbursement Services Managers Group (#1050690)
-- ====================================================================================================================

-- kfs-test-sec37 #T000000000000005469 to group 1050690
insert into krim_grp_mbr_t (grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd)
values ( KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), 
  ( select grp_id from krim_grp_t where grp_nm='UA FSO Disbursement Services Managers'), 
  'T000000000000005469', 'P')
;


-- ====================================================================================================================
-- 19) Add Permissions: Blanket Approve FYPE (#11493), Initiate  Document FYPE (#10126), 
--     Initiate Document LDYE (#10127), Blanket Approve LDYE (#11393), Initiate Document YEGE (#11734) 
--     to UA FSO Fund Accountants Role (#10117), in order to test year end documents.
-- ====================================================================================================================

INSERT  INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10117',
      (select perm_id from krim_perm_t where perm_id ='11493'),'Y')
;

INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10117', 
      (select perm_id from krim_perm_t where perm_id ='10126'),'Y')
;
      
INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10117',
      (select perm_id from krim_perm_t where perm_id ='10127'),'Y')
;

INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10117',
     (select perm_id from krim_perm_t where perm_id ='11393'),'Y') 
;

INSERT INTO krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind) 
    VALUES ( KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), '10117',
     (select perm_id from krim_perm_t where perm_id ='11734'),'Y') 
;




-- ====================================================================================================================
-- 20) De-activate permissions to unmask certain field values on PVEN and PAAT documents (This is only needed until KATTS-1798 is completed.)
-- ====================================================================================================================

-- Remove permissions to unmask certain field values on PVEN and PAAT documents
UPDATE KRIM_ROLE_PERM_T SET ACTV_IND = 'N' 
  WHERE PERM_ID IN ('137','138','128','235','233')
;




-- ====================================================================================================================
-- 21) Add kfs-test-sec15 to the following groups: UA Chart Manager, UA SPS Team 8, UA PACS Postal Services Service Billers, UA SPS Team 7
-- ====================================================================================================================

-- add kfs-test-sec15 to UA Chart Manager Group (#1050703)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values ( KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050703', 'T000000000000005406', 'P')
; 

--OLD add kfs-test-sec15 to UA PACS Postal Services Service Billers Group (#1050639)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values ( KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050639', 'T000000000000005406', 'P')
; 

-- ====================================================================================================================
-- 22) Add 'administer workflow' permission to UA Super User Role (#11513) for Rice Documents (so kfs-test-sec25 can approve any group docs)
--     (Perm #147) Administer Routing for Document Document Type Name : RiceDocument 
-- ====================================================================================================================

-- Add permision Administer Routing for Doc (#147) for UA Super User Role (#11513) 
insert into krim_role_perm_t ( role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind ) 
values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, SYS_GUID(), '11513', '147', 'Y')
;



-- ====================================================================================================================
-- 23a) Add kfs-test-sys10 (Fred Fiscal Officer) as secondary delegate to all accounts for document type KFS (?fin_coa_cd = 'UA)'
-- ====================================================================================================================

--OLD make kfs-test-sys8 (frantic franny) fiscal officer on some accounts 
update kulowner.ca_account_t 
set acct_fsc_ofc_uid = 'T000000000000003187'
where fin_coa_cd = 'UA' and account_nbr in 
('1106200','3001000','3003200','3003900','3004000','3007300','3009600','3011600','3013800','3015000','3016300','3016900','3020200','3024100','3024800','3031300','3032000','3035300','3038900','3039400','3040900','3042700','3042790','3046120','3090300','3093100','3295300','3467300','4506800','4567890','4992000') 
;

--OLD make kfs-test-sys10 #T000000000000003189 fiscal officer on some accounts
update kulowner.ca_account_t 
set acct_fsc_ofc_uid = 'T000000000000003189'
where fin_coa_cd = 'UA' and account_nbr in 
('1732100','2100660','2104180','2104640','2114000','2128000','2141300','2150000','3001400','3188800','3456789','3514200','4074000','4106300','4532800','4996680','4996760')
;

-- ====================================================================================================================
-- 23b)Add kfs-test-sys10 (Fred Fiscal Officer) as secondary delegate to all accounts(Except accounts where already a Fiscal Officer) for document type KFST (KATTS-1994)
-- ====================================================================================================================
--OLD make kfs-test-sys10 #T000000000000003189 secondary delegate on ALL accounts (Except accounts where already a Fiscal Officer)
insert into ca_acct_delegate_t ( fin_coa_cd, account_nbr, fdoc_typ_cd, acct_dlgt_unvl_id, obj_id, ver_nbr, fdoc_aprv_from_amt, acct_dlgt_prmrt_cd, acct_dlgt_actv_cd, acct_dlgt_start_dt, fdoc_aprv_to_amt ) 
select fin_coa_cd, account_nbr, 'KFS', 'T000000000000003189', sys_guid(), 1, null, 'N', 'Y', to_date('2012-03-15', 'YYYY-MM-DD'), null 
from kulowner.ca_account_t 
where fin_coa_cd = 'UA' and not(ACCT_FSC_OFC_UID = 'T000000000000003189')
;


-- ====================================================================================================================
-- 24) Make kfs-test-sec25 a member of UA FSO Security Team Group (#1053022 )
-- ====================================================================================================================

-- add kfs-test-sec25  #T000000000000005416 to Group (#1053022)
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
values ( KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1053022', 'T000000000000005416', 'P')
; 



-- ====================================================================================================================
-- 25) UA PACS Vendor Reviewer Role (#11193) needs perms 137 and 138 as a temp fix till 3) is done
-- ====================================================================================================================

insert into krim_role_perm_t ( role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind ) 
 values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, SYS_GUID(), '11193','137','Y') 
;

insert into krim_role_perm_t ( role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind ) 
 values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, SYS_GUID(), '11193', '138','Y') 
;




------------------------------------------------------------------------------------------------------------------------------
-- Remaining queries from the old script
------------------------------------------------------------------------------------------------------------------------------


-- ====================================================================================================================
-- 26) Add kfs-test-sec12 to groups: UA SPS Team 3 and TEMP - BSA Maintenance Table Managers
-- ====================================================================================================================

-- kfs-test-sec12 #T000000000000005403 in UA SPS Team 3 G#1050092
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050092', 'T000000000000005403', 'P' from dual; 
-- kfs-test-sec12 #T000000000000005403 TEMP - BSA Maintenance Table Managers Group G#1050026
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050026', 'T000000000000005403', 'P' from dual; 


-- ====================================================================================================================
-- 27) Add kfs-test-sec32 to groups: UA SPS Team 3 and TEMP - BSA Maintenance Table Managers
-- ====================================================================================================================


-- kfs-test-sec32 #T000000000000005464 to UA SPS Team 3 G#1050092
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050092', 'T000000000000005464', 'P' from dual; 
-- kfs-test-sec32 #T000000000000005464 to TEMP - BSA Maintenance Table Managers G#1050026
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050026', 'T000000000000005464', 'P' from dual; 




-- ====================================================================================================================
-- 28) Add kfs-test-sec54 to groups: UA FSO Accounts Payable Specialists, UA Development Office and UA FSO AP Travel Specialists
-- ====================================================================================================================

-- kfs-test-sec54 #T000000000000005486 to UA FSO AP Travel Specialists G#1050670
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050670', 'T000000000000005486', 'P' from dual; 
-- kfs-test-sec54 #T000000000000005486 to UA Development Office G#1050042
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050042', 'T000000000000005486', 'P' from dual; 
-- kfs-test-sec54 #T000000000000005486 to UA FSO Accounts Payable Specialists G#1050063
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050063', 'T000000000000005486', 'P' from dual; 



-- ====================================================================================================================
-- 29) Add kfs-test-sec59 to groups: UA Facilities Management PDP Processors and UA Facilities Management Managers
-- ====================================================================================================================

-- kfs-test-sec59 #T000000000000005491 to  UA Facilities Management PDP Processors G#1050043
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050043', 'T000000000000005491', 'P' from dual; 
-- kfs-test-sec59 #T000000000000005491 to  UA Facilities Management Managers G#1050030
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050030', 'T000000000000005491', 'P' from dual;



-- ====================================================================================================================
-- 30) Add kfs-test-sec60 to groups: UA Student Union Managers and UA Student Union PDP Processors
-- ====================================================================================================================

-- kfs-test-sec60 #T000000000000005492 UA Student Union Managers G#1050636
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050636', 'T000000000000005492', 'P' from dual;
-- kfs-test-sec60 #T000000000000005492 UA Student Union PDP Processors G#1050041
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050041', 'T000000000000005492', 'P' from dual; 


-- ====================================================================================================================
-- 31) Add kfs-test-sec36 to groups: UA FSO AP Specialists Team 1 and UA FSO Accounts Payable Specialists
-- ====================================================================================================================

-- Add kfs-test-sec36 #T000000000000005468 to UA FSO AP Specialists Team 1 G#1050643
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050643', 'T000000000000005468', 'P' from dual; 
-- Add kfs-test-sec36 #T000000000000005468 to UA FSO Accounts Payable Specialists G#1050063
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050063', 'T000000000000005468', 'P' from dual; 


-- ====================================================================================================================
-- 32) Add kfs-test-sec14 to groups: UA SPS Team 4, 5, 6
-- ====================================================================================================================

--kfs-test-sec14 #T000000000000005405 to UA SPS Team 4 G#1050637
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050637', 'T000000000000005405', 'P' from dual; 
--kfs-test-sec14 #T000000000000005405 to UA SPS Team 5 G#1050076
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050076', 'T000000000000005405', 'P' from dual; 
--kfs-test-sec14 #T000000000000005405 to UA SPS Team 6 G#1050049
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050049', 'T000000000000005405', 'P' from dual; 


-- ====================================================================================================================
-- 33) Add kfs-test-sec58 to groups: UA Bookstore PDP Processors, UA Bookstore Managers
-- ====================================================================================================================

-- kfs-test-sec58 #T000000000000005490 to UA Bookstore PDP Processors G#1050080
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050080', 'T000000000000005490', 'P' from dual; 
-- kfs-test-sec58 #T000000000000005490 to UA Bookstore Managers G#1050051
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050051', 'T000000000000005490', 'P' from dual;



-- ====================================================================================================================
-- 34) Add kfs-test-sec68 to groups: UA FSO 1099 Tax Specialists
-- ====================================================================================================================

-- kfs-test-sec68 #T000000000000005506 to UA FSO 1099 Tax Specialists G#1050667
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050667', 'T000000000000005506', 'P' from dual; 
-- kfs-test-sec68 #T000000000000005506 to UA FSO Accounts Payable Tax Specialists G#1050031
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050031', 'T000000000000005506', 'P' from dual; 


-- ====================================================================================================================
-- 35) Asigning test users to groups
-- ====================================================================================================================

-- kfs-test-sec28 #T000000000000005460 to UA FSO Bursar's Office Banking Reps G#1050081
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050081', 'T000000000000005460', 'P' from dual; 

-- kfs-test-sec3 #T000000000000005394 to UA PACS Disbursement Voucher Review G#1050638
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050638', 'T000000000000005394', 'P' from dual; 

-- kfs-test-sec30 #T000000000000005462 to UA FSO Student Financial Aid G#1050073
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050073', 'T000000000000005462', 'P' from dual; 

-- kfs-test-sec39 #T000000000000005471 to UA Internal Auditors G#1050084
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050084', 'T000000000000005471', 'P' from dual; 

-- kfs-test-sec48 #T000000000000005480 to UA FSO FM Compliance G#1050072
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050072', 'T000000000000005480', 'P' from dual; 

-- kfs-test-sec5 #T000000000000005396 to UA SPS Property Managers G#1050061
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050061', 'T000000000000005396', 'P' from dual; 

-- kfs-test-sec13 #T000000000000005404 to UA FSO Bursar's Office Reps G#1050032
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050032', 'T000000000000005404', 'P' from dual; 

-- kfs-test-sec11 #T000000000000005402 to UA SPS Team 2 G#1050039
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050039', 'T000000000000005402', 'P' from dual; 

-- kfs-test-sec29 #T000000000000005461 to UA FSO Endowments G#1050035
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050035', 'T000000000000005461', 'P' from dual; 

-- kfs-test-sec45 #T000000000000005477 to UA PACS Printing Service Billers G#1050075
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050075', 'T000000000000005477', 'P' from dual; 

-- kfs-test-sec50 #T000000000000005482 to UA PACS Vendor Managers G#1050090
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050090', 'T000000000000005482', 'P' from dual; 

-- kfs-test-sec55 #T000000000000005487 to UA Development Office G#1050042
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050042', 'T000000000000005487', 'P' from dual; 

-- kfs-test-sec67 #T000000000000005505 to UA FSO FM Accounting Managers G#1050668
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050668', 'T000000000000005505', 'P' from dual; 

-- kfs-test-sec1 #T000000000000005392 to TEMP - Parameter Managers G#1050028
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050028', 'T000000000000005392', 'P' from dual; 

-- kfs-test-sec23 #T000000000000005414 to UA FSO FM Team 452 G#1050055
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050055', 'T000000000000005414', 'P' from dual; 

-- kfs-test-sec27 #T000000000000005459 to UA FSO Cash Accounting G#1050034
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050034', 'T000000000000005459', 'P' from dual; 

-- kfs-test-sec4 #T000000000000005395 to UA Budget Office Organization Managers G#1050062
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050062', 'T000000000000005395', 'P' from dual; 

-- kfs-test-sec63 #T000000000000005501 to UA FSO FM Tax Compliance G#1050649
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050649', 'T000000000000005501', 'P' from dual; 

-- kfs-test-sec69 #T000000000000005507 to UA SPS Operational Advance Review G#1050669
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050669', 'T000000000000005507', 'P' from dual; 

-- kfs-test-sec71 #T000000000000005509 to UA University Animal Care G#1050675
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050675', 'T000000000000005509', 'P' from dual; 

-- kfs-test-sec19 #T000000000000005410 to UA PACS PCard Administrators G#1050059
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050059', 'T000000000000005410', 'P' from dual; 

-- kfs-test-sec44 #T000000000000005476 to UA FSO Capital Finance DEBT Approvers G#1050064
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050064', 'T000000000000005476', 'P' from dual; 

-- kfs-test-sec46 #T000000000000005478 to UA PACS Stores Service Billers G#1050078
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050078', 'T000000000000005478', 'P' from dual; 

-- kfs-test-sec57 #T000000000000005489 to UA ORCA Managers G#1050095
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050095', 'T000000000000005489', 'P' from dual; 

-- kfs-test-sec62 #T000000000000005500 to UA PACS Central Receiving Service Biller G#1050645
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050645', 'T000000000000005500', 'P' from dual; 

-- kfs-test-sec16 #T000000000000005407 to UA FSO Bursar's Office Managers G#1050046
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050046', 'T000000000000005407', 'P' from dual; 

-- kfs-test-sec2 #T000000000000005393 to UA Asset Custodians G#1050093
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050093', 'T000000000000005393', 'P' from dual; 

-- kfs-test-sec31 #T000000000000005463 to UA FSO Payroll Accounting G#1050077
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050077', 'T000000000000005463', 'P' from dual; 

-- kfs-test-sec49 #T000000000000005481 to UA Office of the General Counsel G#1050074
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050074', 'T000000000000005481', 'P' from dual; 

-- kfs-test-sec53 #T000000000000005485 to UA Radiation Control Office G#1050048
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050048', 'T000000000000005485', 'P' from dual; 

-- kfs-test-sec65 #T000000000000005503 to UA SPS Pre-Award G#1050648
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050648', 'T000000000000005503', 'P' from dual; 

-- kfs-test-sec21 #T000000000000005412 to UA FSO FM Team 450 G#1050054
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050054', 'T000000000000005412', 'P' from dual; 

-- kfs-test-sec42 #T000000000000005474 to UA Budget Office Local Fund Managers G#1050069
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050069', 'T000000000000005474', 'P' from dual; 

-- kfs-test-sec56 #T000000000000005488 to UA Office of Contract Research Analysis G#1050068
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050068', 'T000000000000005488', 'P' from dual; 

-- kfs-test-sec61 #T000000000000005499 to UA SPS Kuali Coeus Administrators G#1050642
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050642', 'T000000000000005499', 'P' from dual; 

-- kfs-test-sec10 #T000000000000005401 to UA SPS Team 1 G#1050091
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050091', 'T000000000000005401', 'P' from dual; 

-- kfs-test-sec17 #T000000000000005408 to UA FSO Administration G#1050044
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050044', 'T000000000000005408', 'P' from dual; 

-- kfs-test-sec18 #T000000000000005409 to UA PACS Surplus Property G#1050089
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050089', 'T000000000000005409', 'P' from dual; 

-- kfs-test-sec20 #T000000000000005411 to UA PACS Managers G#1050086
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050086', 'T000000000000005411', 'P' from dual; 

-- kfs-test-sec24 #T000000000000005415 to UA FSO FM Team 453 G#1050088
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050088', 'T000000000000005415', 'P' from dual; 

-- kfs-test-sec26 T000000000000005458 to UA FSO FM Rate Studies G#1050066
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050066', 'T000000000000005458', 'P' from dual; 

-- kfs-test-sec33 #T000000000000005465 to UA SPS Managers G#1050060
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050060', 'T000000000000005465', 'P' from dual;

-- kfs-test-sec34 #T000000000000005466 to UA SPS Prorate JE Managers G#1050038
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050038', 'T000000000000005466', 'P' from dual; 

-- kfs-test-sec38 #T000000000000005470 to UA FSO Prorate JE Managers G#1050056
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050056', 'T000000000000005470', 'P' from dual; 

-- kfs-test-sec47 #T000000000000005479 to UA PACS Administration G#1050085
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050085', 'T000000000000005479', 'P' from dual; 

-- kfs-test-sec22 #T000000000000005413 to UA FSO FM Team 451 G#1050083
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050083', 'T000000000000005413', 'P' from dual; 

-- kfs-test-sec35 #T000000000000005467 to UA FSO Capital Finance G#1050053
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050053', 'T000000000000005467', 'P' from dual; 

-- kfs-test-sec43  #T000000000000005475 to UA Budget Office State Fund Managers G#1050052
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050052', 'T000000000000005475', 'P' from dual; 

-- kfs-test-sec51 #T000000000000005483 to UA FSO Accounts Payable Check Control G#1050070
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050070', 'T000000000000005483', 'P' from dual; 

-- kfs-test-sec52 #T000000000000005484 to UA FSO Accounts Payable Tax Specialists G#1050031
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050031', 'T000000000000005484', 'P' from dual; 

-- kfs-test-sec6 #T000000000000005397 to UA FSO Property Management G#1050036
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050036', 'T000000000000005397', 'P' from dual; 

-- kfs-test-sec7 #T000000000000005398 to UA FSO Capital Finance Managers G#1050033
insert into krim_grp_mbr_t ( grp_mbr_id, ver_nbr, obj_id, grp_id, mbr_id, mbr_typ_cd ) 
select KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), '1050033', 'T000000000000005398', 'P' from dual; 

UPDATE KULOWNER.KRCR_PARM_T SET VAL = 'Y' WHERE PARM_NM = 'SHOW_BACK_DOOR_LOGIN_IND';

-- ====================================================================================================================
-- Add Permission/Role for Backdoor Access Control
-- ====================================================================================================================

-- Create permission "Use Backdoor Log In Kuali Portal" (UAF-94)
insert into krim_perm_t (perm_id, obj_id, ver_nbr, perm_tmpl_id, nmspc_cd, nm, desc_txt, actv_ind)
values (KRIM_PERM_ID_S.NEXTVAL, sys_guid(), 1, '1', 'KR-SYS', 'Use Backdoor Log In Kuali Portal', 'Use Backdoor Log In Kuali Portal', 'Y');

-- Create role "Back Door Login"
insert into krim_role_t (role_id, obj_id, ver_nbr, role_nm, nmspc_cd, desc_txt, kim_typ_id, actv_ind)
values (KRIM_ROLE_ID_S.NEXTVAL, sys_guid(), 1, 'Back Door Login', 'KR-SYS', 'Back Door Login', '1', 'Y');

-- Add Permission to the Role
insert into krim_role_perm_t (role_perm_id, ver_nbr, obj_id, role_id, perm_id, actv_ind)
values (KRIM_ROLE_PERM_ID_S.NEXTVAL, 1, sys_guid(), 
  (select role_id from krim_role_t where role_nm='Back Door Login'), 
  (select perm_id from krim_perm_t where nmspc_cd='KR-SYS' and desc_txt='Use Backdoor Log In Kuali Portal'),
  'Y')
;

-- Add KFS Developers to Role to BackDoor Login
-- Look up new UAIDs from NetIDs at https://toolshed.uits.arizona.edu/lookup/eds/
-- kbasu
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '105281498340', 'P');
-- gurtonj
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '115466589719', 'P');
-- akhilashokk
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '116175174074', 'P');
-- elvirag
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '101969935231', 'P');
-- jpfennig
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '114187875519', 'P');
-- kosta
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '117013605279', 'P');
-- jwingate
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '120566406456', 'P');
-- mccunej
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '103818749727', 'P');
-- maryb
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '113307485249', 'P');
-- sandover
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '103357749183', 'P');
-- kbrook
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '116836748332', 'P');
-- robbiem
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '109455635680', 'P');
-- fischerm
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '114428387134', 'P');
-- mmoen
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '105010108595', 'P');
-- ketchmark
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '107510420325', 'P');
-- lilas
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '109449915915', 'P');
-- jnschool
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '117902017578', 'P');
-- melissav73
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '100503195705', 'P');
-- sskinner
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '104454139762', 'P');
-- rdubisar
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '104456668368', 'P');
-- marcel
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '115008061109', 'P');
-- janifisk
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '117390795312', 'P');
-- sandberm
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '117788723126', 'P');
-- mhohl
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '101597768319', 'P');
-- ake27
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '117498812436', 'P');
-- hlo
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '104758384661', 'P');
-- alee
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '102452189233', 'P');
-- perryg
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '101960720347', 'P');
-- maramian
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '110323772179', 'P');
-- rblank
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '101578396693', 'P');
-- tammyv
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '109268636888', 'P');
-- buchanaw
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '103030668774', 'P');
-- jreese
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '114914607687', 'P');
-- quikkian
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '104309117645', 'P');
-- tounou
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '100108997730', 'P');
-- amyrodriguez
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '112956025383', 'P');
-- wames
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '103935099944', 'P');
-- nataliac
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '113030184588', 'P');
-- mwellman
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '105481261747', 'P');
-- mcalopez
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '110188740559', 'P');
-- arohr
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '109219495500', 'P');
-- eahowden
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '100278800517', 'P');
-- amorrell
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '113204291305', 'P');
-- amandazhang
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '119780814779', 'P');
-- naira
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '101165212051', 'P');
-- cdkanzig 
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '100440189667', 'P');
-- kruser
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '121106547226', 'P');
-- lmclaugh
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '112513191671', 'P');
-- zavala
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '114776665069', 'P');
-- lee4
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '116177935604', 'P');
-- jcaddel
insert into krim_role_mbr_t ( role_mbr_id, ver_nbr, obj_id, role_id, mbr_id, mbr_typ_cd) values (KRIM_ROLE_MBR_ID_S.NEXTVAL, 1, SYS_GUID(), (select role_id from krim_role_t where role_nm='Back Door Login'), '118368674752', 'P');

-- ======================================================================================================
-- New EDS Parameters to be added (UAF-320)
-- ======================================================================================================

insert into KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) 
values ('KUALI','KR-SYS','Config','KIM_TO_LDAP_FIELD_MAPPINGS',1,'CONFG',
'entityId=uaid;principalId=uaid;principalName=uid;givenName=sn;principals.principalName=uid;principals.principalId=uaid;principals.active=eduPersonAffiliation;lastName=sn;firstName=givenName;employmentInformation.employeeStatus=employeeStatus;employmentInformation.employeeId=emplId;names.lastName=sn;names.firstName=givenName;employmentInformation.employeeStatusCode=employeeStatus;employmentInformation.primaryDepartmentCode=employeeOfficialOrg,employeePoiPrimaryDept,dccPrimaryDept;',
'Maps a KIM field/property name to an EDS field/property name','A',SYS_GUID())
;

insert into KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) 
values ('KUALI','KR-SYS','Config','KIM_TO_LDAP_UNMAPPED_FIELDS',1,'CONFG','entityTypes.entityTypeCode;','KIM Fields that have no mapping in EDS','A',SYS_GUID())
;

insert into KRCR_PARM_T (APPL_ID,NMSPC_CD,CMPNT_CD,PARM_NM,VER_NBR,PARM_TYP_CD,VAL,PARM_DESC_TXT,EVAL_OPRTR_CD,OBJ_ID) 
values ('KUALI','KR-SYS','Config','KIM_TO_LDAP_VALUE_MAPPINGS',1,'CONFG',
'principals.active.Y=dcc,poi,affiliate,employee,faculty,staff;principals.active.N=admit,former-employee,former-faculty,former-member,former-staff,former-student,!*;','Maps a KIM field/property name to an EDS field/property name','A',SYS_GUID())
;
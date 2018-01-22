--------------------------------------------------------------------------------
-- UAF-6591
-- Add an authorizer for the CountyMaintenanceDocument, StateMaintenanceDocumentdocuments, PostalCodeMaintenanceDocument , and CountryMaintenanceDocument
-- These updates only affect documents with parents (parnt_id) that are different than the most current version and will allow them to be opened in kfs7
--------------------------------------------------------------------------------
update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID not in (select PARNT_ID from KREW_DOC_TYP_T where CUR_IND = 1 and DOC_TYP_NM ='CountyMaintenanceDocument')
    and DOC_TYP_NM = 'CountyMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID not in (select PARNT_ID from KREW_DOC_TYP_T where CUR_IND = 1 and DOC_TYP_NM ='StateMaintenanceDocument')
    and DOC_TYP_NM = 'StateMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID not in (select PARNT_ID from KREW_DOC_TYP_T where CUR_IND = 1 and DOC_TYP_NM ='PostalCodeMaintenanceDocument')
    and DOC_TYP_NM = 'PostalCodeMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID not in (select PARNT_ID from KREW_DOC_TYP_T where CUR_IND = 1 and DOC_TYP_NM ='CountryMaintenanceDocument')
    and DOC_TYP_NM = 'CountryMaintenanceDocument';
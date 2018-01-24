--------------------------------------------------------------------------------
-- UAF-6591
-- Add an authorizer for the CountyMaintenanceDocument, StateMaintenanceDocumentdocuments, PostalCodeMaintenanceDocument , and CountryMaintenanceDocument
-- These updates only affect documents with parents (parnt_id) that are different than the most current version and will allow them to be opened in kfs7
--------------------------------------------------------------------------------
update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID = '329519'
    and DOC_TYP_NM = 'CountyMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID = '329519'
    and DOC_TYP_NM = 'StateMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID = '329519'
    and DOC_TYP_NM = 'PostalCodeMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where PARNT_ID = '329519'
    and DOC_TYP_NM = 'CountryMaintenanceDocument';
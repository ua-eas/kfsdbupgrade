--------------------------------------------------------------------------------
-- UAF-6591
-- Add an authorizer for the CountyMaintenanceDocument, StateMaintenanceDocumentdocuments, PostalCodeMaintenanceDocument , and CountryMaintenanceDocument
-- These updates only affect documents with parents (parnt_id) that are different than the most current version and will allow them to be opened in kfs7
--------------------------------------------------------------------------------
update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where DOC_TYP_ID in ('330623','320488')
    and DOC_TYP_NM = 'CountyMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where DOC_TYP_ID in ('330627','320494')
    and DOC_TYP_NM = 'StateMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where DOC_TYP_ID in ('330624','320493')
    and DOC_TYP_NM = 'PostalCodeMaintenanceDocument';

update KREW_DOC_TYP_T
    set AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer'
    where DOC_TYP_ID in ('330625','320487')
    and DOC_TYP_NM = 'CountryMaintenanceDocument';
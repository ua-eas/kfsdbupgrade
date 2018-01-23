--------------------------------------------------------------------------------
-- UAF-6631
-- Add an doc_hndlr_url and authorizer for DocumentTypeDocument, ParameterDetailTypeMaintenanceDocument and ParameterMaintenanceDocument
--------------------------------------------------------------------------------
UPDATE KREW_DOC_TYP_T
    SET AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer', DOC_HDLR_URL='${kr.url}/maintenance.do?methodToCall=docHandler'
    where DOC_TYP_NM = 'DocumentTypeDocument' AND CUR_IND = 0;
    
UPDATE KREW_DOC_TYP_T
    SET AUTHORIZER = 'org.kuali.rice.kew.doctype.service.impl.KimDocumentTypeAuthorizer', DOC_HDLR_URL='${kr.url}/maintenance.do?methodToCall=docHandler'
    where DOC_TYP_NM = 'ParameterMaintenanceDocument' AND CUR_IND = 0;

--  ParameterDetailTypeMaintenanceDocument is discountinued completely in Rice 2.5.19, no BO, nor Doc DD, neither OJB mapping. 
--  Map to document type to ComponentMaintenanceDocument which is replacement of ParameterDetailTypeMaintenanceDocument in Rice 2.5.19 
-- legacy doc_typ_id is 320489
UPDATE KREW_DOC_HDR_T S
SET DOC_TYP_ID =
  (SELECT T.DOC_TYP_ID
  FROM KREW_DOC_TYP_T T
  WHERE T.DOC_TYP_NM='ComponentMaintenanceDocument'
  AND T.CUR_IND     =1
  )
WHERE S.DOC_TYP_ID IN
  (SELECT DOC_TYP_ID
  FROM KREW_DOC_TYP_T T
  WHERE T.DOC_TYP_NM='ParameterDetailTypeMaintenanceDocument'
  );
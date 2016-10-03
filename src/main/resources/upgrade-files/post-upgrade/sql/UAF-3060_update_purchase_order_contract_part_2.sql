-- =============================================================================================================
-- (UAF-3060) : Update Purchase Order Contract Language Maintenance Table (PMCL) Part 2. Subtask of (UAF-92)
-- =============================================================================================================
UPDATE KULOWNER.PUR_PO_CONTR_LANG_T SET PO_CONTR_LANG_DESC = 
'UA Purchase Order Terms and Conditions:' || chr(10) || 
'http://pacs.arizona.edu/po-terms' || chr(10) || chr(10) || 
'2 Vendor Instructions:' || chr(10) || 
'a. Acceptance of this order includes acceptance of all terms and conditions available at the above' || chr(10) || 
'link.' || chr(10) || 
'b. Price increases will not be recognized without written notice and acceptance by Purchasing.' || chr(10) || 
'c. Please itemize all charges on your invoice and reference the PO number.' || chr(10) || 
'd. University is exempt from federal excise tax. Certificate of registry is A-184524.' || chr(10) || 
'e. Transaction privilege tax no. 20221243.' || chr(10) || 
'f. Out of state vendors charging Arizona sales tax must show permit number.' || chr(10) || 
'g. For invoicing and payment information contact Accounts Payable at address above or at' || chr(10) || 
'accts_pay@fso.arizona.edu. Fax invoices to 520-626-1243 or email invoices to invoices@fso.arizona.edu.'
WHERE DOBJ_MAINT_CD_ACTV_IND = 'Y'
AND CMP_CD IN ('MC','AH','OC','SV','PX','AE','AG');

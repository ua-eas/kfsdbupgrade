-- ======================================================================
--  Create FSO Web Page for PO Terms and Conditions & 
--      Update Purchase Order Contract Language Maintenance Table (PMCL)
--  (UAF-95) subtask of (UAF-92)
-- ======================================================================
UPDATE PUR_PO_CONTR_LANG_T SET PO_CONTR_LANG_DESC ='UA Purchase Order Terms and Conditions:
http://pacs.arizona.edu/po-terms
2 Vendor Instructions:
a. Acceptance of this order includes acceptance of all terms and conditions available at the above
link.
b. Price increases will not be recognized without written notice and acceptance by Purchasing.
c. Please itemize all charges on your invoice and reference the PO number.
d. University is exempt from federal excise tax. Certificate of registry is A-184524.
e. Transaction privilege tax no. 20221243.
f. Out of state vendors charging Arizona sales tax must show permit number.
g. For invoicing and payment information contact Accounts Payable at address above or at
accts_pay@fso.arizona.edu. Fax invoices to 520-626-1243 or email invoices to invoices@fso.arizona.edu' WHERE DOBJ_MAINT_CD_ACTV_IND = 'Y' AND CMP_CD IN ('MC','AH','OC','SV');

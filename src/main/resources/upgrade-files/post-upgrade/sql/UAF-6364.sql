--------------------------------------------------------------------------------
--   UAF-6364 : Update Vendor Review Page Text Values on Vendor Type maintenance table (Subtask of UAF-92)
--------------------------------------------------------------------------------
update pur_vndr_typ_t
set vndr_rvw_txt ='Accounts Payable will obtain the Vendor''s IRS W-9 form when required to initiate payment.[br]In an effort to expedite the payment please forward the W-9 form to Accounts Payable [br] via fax at 520-626-1243 and reference the e-Doc number.[br] Original ICON, Performance Agreement or W-8BEN should be forwarded to Accounts Payable via the DV Cover sheet when payment is made.[br][br]Inquiries regarding vendor related questions,[br] please contact Accounts Payable at 520-621-9097 or email to: accts_pay@fso.arizona.edu[br][br] Click "yes" to continue "no" to return to the document.'
where VNDR_TYP_CD='DV';

update pur_vndr_typ_t
set vndr_rvw_txt='In order to complete the vendor set up, please mail the vendor''s W-9 Tax form via campus mail to[br] FSO/Operations, PO BOX 210158, Campus or in emergency situations via fax to (520) 626-1243. [br]Reference e-doc number {1}. [br][br] For inquiries regarding vendor-related requests, [br] contact Accounts Payable at 520-621-9097 or VendorMaintenance@fso.arizona.edu [br] [br] Click "yes" to continue "no" to return to the document.'
where VNDR_TYP_CD='PO';
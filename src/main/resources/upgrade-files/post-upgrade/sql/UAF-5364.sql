-- =======================================================================================================================
-- UAF-5364 : Add Permission to modify invoiceNumber on Tax route node on DV and assign to Document Editor role
-- =======================================================================================================================

-- Add Permission 'Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber' and assign it to the Document Editor role
INSERT INTO KRIM_PERM_T (PERM_ID, OBJ_ID, VER_NBR, PERM_TMPL_ID, NMSPC_CD, NM, DESC_TXT, ACTV_IND) 
 VALUES (KRIM_PERM_ID_S.NEXTVAL, 'UAF-5364-1', '1', '41', 'KFS-FP', 'Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber', 'Allows users to modify the invoice number of Source accounting lines on a Disbursement Voucher document that is at the Tax Node of routing.', 'Y');

INSERT INTO KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID, OBJ_ID, VER_NBR, PERM_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, 'UAF-5364-21', '1', (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber' AND OBJ_ID='UAF-5364-1'), '52', '13', 'DV');

INSERT INTO KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID, OBJ_ID, VER_NBR, PERM_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, 'UAF-5364-22', '1', (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber' AND OBJ_ID='UAF-5364-1'), '52', '16', 'Tax');

INSERT INTO KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID, OBJ_ID, VER_NBR, PERM_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, 'UAF-5364-23', '1', (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber' AND OBJ_ID='UAF-5364-1'), '52', '6', 'sourceAccountingLines.extension.invoiceNumber');

INSERT INTO KRIM_ROLE_PERM_T (ROLE_PERM_ID, OBJ_ID, VER_NBR, ROLE_ID, PERM_ID, ACTV_IND) 
 VALUES (KRIM_ROLE_PERM_ID_S.NEXTVAL, 'UAF-5364-3', '1', '66', (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Modify Accounting Lines DV Tax sourceAccountingLines.extension.invoiceNumber' AND OBJ_ID='UAF-5364-1'), 'Y');



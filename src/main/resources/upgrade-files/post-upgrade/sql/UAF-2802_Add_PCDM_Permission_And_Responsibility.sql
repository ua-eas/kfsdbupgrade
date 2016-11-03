-- =======================================================================================================================
-- UAF-2802 : Add Permission to create PCDM to UA PCard Administrator so routing should go directly to final without routing
-- =======================================================================================================================

-- Add Permission 'Initiate Document PCDM' and assign it to the UA PCard Administrator role
INSERT INTO KRIM_PERM_T (PERM_ID, PERM_TMPL_ID, NM, NMSPC_CD, VER_NBR, DESC_TXT, OBJ_ID, ACTV_IND) 
 VALUES (KRIM_PERM_ID_S.NEXTVAL, '10', 'Initiate Document PCDM', 'KFS-FP', '1', 'Authorizes the initiation of the Procurement Cardholder Maintenance Document.', 'UAF-2802-1', 'Y');

INSERT INTO KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID, PERM_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL, VER_NBR, OBJ_ID) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Initiate Document PCDM' AND OBJ_ID='UAF-2802-1'), '3', '13', 'PCDM', '1', 'UAF-2802-2');

INSERT INTO KRIM_ROLE_PERM_T (ROLE_PERM_ID, PERM_ID, ROLE_ID, VER_NBR, OBJ_ID, ACTV_IND) 
 VALUES (KRIM_ROLE_PERM_ID_S.NEXTVAL, (SELECT PERM_ID FROM KRIM_PERM_T WHERE NM='Initiate Document PCDM' AND OBJ_ID='UAF-2802-1'), (SELECT ROLE_ID from KRIM_ROLE_T WHERE ROLE_NM='UA PCard Administrator'), '1', 'UAF-2802-3', 'Y');


 -- Adding responsibility without an assigned group for PCDM routing should go directly to final without routing
INSERT INTO KRIM_RSP_T (RSP_ID, RSP_TMPL_ID, NM, NMSPC_CD, VER_NBR, DESC_TXT, OBJ_ID, ACTV_IND) 
 VALUES (KRIM_RSP_ID_S.NEXTVAL, '1', 'Review PCDM CentralAdministrationReview', 'KFS-FP', '1', 'Enforce a central office review of any changes to the Procurement Cardholder maintenance documents. This will not be assigned to anyone, since PCARD admin has initiate and do not want the doc to route.', 'UAF-2802-4', 'Y');

INSERT INTO KRIM_RSP_ATTR_DATA_T (ATTR_DATA_ID, RSP_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL, VER_NBR, OBJ_ID) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, (SELECT RSP_ID FROM KRIM_RSP_T WHERE NM='Review PCDM CentralAdministrationReview' AND OBJ_ID='UAF-2802-4'), '7', '41', 'false', '1', 'UAF-2802-5');

INSERT INTO KRIM_RSP_ATTR_DATA_T (ATTR_DATA_ID, RSP_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL, VER_NBR, OBJ_ID) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, (SELECT RSP_ID FROM KRIM_RSP_T WHERE NM='Review PCDM CentralAdministrationReview' AND OBJ_ID='UAF-2802-4'), '7', '16', 'CentralAdministrationReview', '1', 'UAF-2802-6');

INSERT INTO KRIM_RSP_ATTR_DATA_T (ATTR_DATA_ID, RSP_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL, VER_NBR, OBJ_ID) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, (SELECT RSP_ID FROM KRIM_RSP_T WHERE NM='Review PCDM CentralAdministrationReview' AND OBJ_ID='UAF-2802-4'), '7', '13', 'PCDM', '1', 'UAF-2802-7');

INSERT INTO KRIM_RSP_ATTR_DATA_T (ATTR_DATA_ID, RSP_ID, KIM_TYP_ID, KIM_ATTR_DEFN_ID, ATTR_VAL, VER_NBR, OBJ_ID) 
 VALUES (KRIM_ATTR_DATA_ID_S.NEXTVAL, (SELECT RSP_ID FROM KRIM_RSP_T WHERE NM='Review PCDM CentralAdministrationReview' AND OBJ_ID='UAF-2802-4'), '7', '40', 'false', '1', 'UAF-2802-8');

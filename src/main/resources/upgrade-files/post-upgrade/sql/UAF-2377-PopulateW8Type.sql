-- =================================================================================================
--  Insert W-8 type maintenance table values during database upgrade (UAF-2377) subtask of (UAF-92)
-- =================================================================================================
INSERT INTO PUR_VNDR_W8_TYP_T (VNDR_W8_TYP_CD, OBJ_ID, VER_NBR, VNDR_W8_TYP_DESC, ACTV_IND) VALUES ('W1', SYS_GUID(), 1, 'W-8BEN', 'Y');
INSERT INTO PUR_VNDR_W8_OWNRSHP_T (ID, OBJ_ID, VER_NBR, VNDR_W8_TYP_CD, VNDR_OWNRSHIP_CD, ACTV_IND) VALUES ('W1', SYS_GUID(), 1, 'W1', 'FV', 'Y');
INSERT INTO PUR_VNDR_W8_TYP_T (VNDR_W8_TYP_CD, OBJ_ID, VER_NBR, VNDR_W8_TYP_DESC, ACTV_IND) VALUES ('W2', SYS_GUID(), 1, 'W-8BEN-E', 'Y');
INSERT INTO PUR_VNDR_W8_OWNRSHP_T (ID, OBJ_ID, VER_NBR, VNDR_W8_TYP_CD, VNDR_OWNRSHIP_CD, ACTV_IND) VALUES ('W2', SYS_GUID(), 1, 'W2', 'FV', 'Y');
INSERT INTO PUR_VNDR_W8_TYP_T (VNDR_W8_TYP_CD, OBJ_ID, VER_NBR, VNDR_W8_TYP_DESC, ACTV_IND) VALUES ('W3', SYS_GUID(), 1, 'W-8ECI', 'Y');
INSERT INTO PUR_VNDR_W8_OWNRSHP_T (ID, OBJ_ID, VER_NBR, VNDR_W8_TYP_CD, VNDR_OWNRSHIP_CD, ACTV_IND) VALUES ('W3', SYS_GUID(), 1, 'W3', 'FV', 'Y');
INSERT INTO PUR_VNDR_W8_TYP_T (VNDR_W8_TYP_CD, OBJ_ID, VER_NBR, VNDR_W8_TYP_DESC, ACTV_IND) VALUES ('W4', SYS_GUID(), 1, 'W-8EXP', 'Y');
INSERT INTO PUR_VNDR_W8_OWNRSHP_T (ID, OBJ_ID, VER_NBR, VNDR_W8_TYP_CD, VNDR_OWNRSHIP_CD, ACTV_IND) VALUES ('W4', SYS_GUID(), 1, 'W4', 'FV', 'Y');
INSERT INTO PUR_VNDR_W8_TYP_T (VNDR_W8_TYP_CD, OBJ_ID, VER_NBR, VNDR_W8_TYP_DESC, ACTV_IND) VALUES ('W5', SYS_GUID(), 1, 'W-8IMY', 'Y');
INSERT INTO PUR_VNDR_W8_OWNRSHP_T (ID, OBJ_ID, VER_NBR, VNDR_W8_TYP_CD, VNDR_OWNRSHIP_CD, ACTV_IND) VALUES ('W5', SYS_GUID(), 1, 'W5', 'FV', 'Y');
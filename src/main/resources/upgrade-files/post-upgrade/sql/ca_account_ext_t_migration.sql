-- =========================================================================================
-- Adds FUNDS_TYPE_CD column to the Account table (CA_ACCOUNT_T) and then copies the funds 
-- type code values from the Account Extension table (CA_ACCOUNT_EXT_T) to the Account table 
-- because the Account Extension table will no longer be used in KFS 6.0  (UAF-2404) 
-- =========================================================================================
ALTER TABLE CA_ACCOUNT_T ADD FUNDS_TYPE_CD VARCHAR(3);
ALTER TABLE CA_ACCOUNT_T ADD FOREIGN KEY (FUNDS_TYPE_CD) REFERENCES CA_SOURCE_OF_FUNDS_T(FUNDS_TYPE_CD);

UPDATE CA_ACCOUNT_T A
SET A.FUNDS_TYPE_CD = (
SELECT B.FUNDS_TYPE_CD
FROM CA_ACCOUNT_EXT_T B
WHERE B.FIN_COA_CD = A.FIN_COA_CD AND B.ACCOUNT_NBR = A.ACCOUNT_NBR);
-- =======================================================================================================================
--  UAF-4294 : FP_PRCRMNT_CARD_DFLT_T replace primary key to credit card number instead of id
-- =======================================================================================================================

--drop id PK
ALTER TABLE KULOWNER.FP_PRCRMNT_CARD_DFLT_T DROP CONSTRAINT FP_PRCRMNT_CARD_DFLT_TP1;

--add cc_numer PK constraint
ALTER TABLE KULOWNER.FP_PRCRMNT_CARD_DFLT_T ADD CONSTRAINT FP_PRCRMNT_CARD_DFLT_TP1 PRIMARY KEY (CC_NBR);

--ad unique id constraint
ALTER TABLE KULOWNER.FP_PRCRMNT_CARD_DFLT_T ADD CONSTRAINT FP_PRCRMNT_CARD_DFLT_TP2 UNIQUE (ID);
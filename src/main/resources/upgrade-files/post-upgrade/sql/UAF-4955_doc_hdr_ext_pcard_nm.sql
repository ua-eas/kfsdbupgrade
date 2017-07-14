-- UAF-4955 Renaming several fields to match earlier mod which introduced sane names Also,
--          this used to be liquibase, but proved to be too long running to keep there. A
--          temporary index is used as an optimization, taking the run time from 6 hours
--          down to 34m.
CREATE INDEX KREW_DOC_HDR_EXT_TI4 ON KREW_DOC_HDR_EXT_T (KEY_CD);
UPDATE KREW_DOC_HDR_EXT_T SET KEY_CD = 'creditCardLastFour' WHERE KEY_CD = 'cardApprovalOfficial';
UPDATE KREW_DOC_HDR_EXT_T SET KEY_CD = 'name' WHERE KEY_CD = 'groupName' AND VAL like 'UA PCRD%';
DROP INDEX KREW_DOC_HDR_EXT_TI4;

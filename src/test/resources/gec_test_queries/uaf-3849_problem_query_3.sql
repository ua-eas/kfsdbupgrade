drop table FP_GEC_ENTRY_REL_T;

--------------------------------------------------------------------------------------------------------------------------
-- 06/12 - Create new GEC Entry Relationship table. Note, the gec_fdoc_ref_nbr field is for backend work,
--------------------------------------------------------------------------------------------------------------------------
-- The line UUID is populated in-app, so we _don't_ want to default it's value here.
CREATE TABLE FP_GEC_ENTRY_REL_T  (
    ENTRY_ID            NUMBER(19,0),
    GEC_FDOC_NBR        VARCHAR2(14),
    GEC_FDOC_REF_NBR    VARCHAR2(14),
    GEC_FDOC_LN_TYP_CD  VARCHAR2(1),
    GEC_ACCT_LINE_NBR   NUMBER(7,0),
    GEC_ACCT_LINE_UUID  VARCHAR2(36 BYTE),
    GEC_DOC_HDR_STAT_CD VARCHAR2(1),
    VER_NBR             NUMBER(8,0) DEFAULT 1 NOT NULL ENABLE,
    OBJ_ID              VARCHAR2(36 BYTE) DEFAULT SYS_GUID() NOT NULL ENABLE
);


--------------------------------------------------------------------------------------------------------------------------
-- 07/12 - Populate the new GEC Entry Relationship table.
--------------------------------------------------------------------------------------------------------------------------
-- For every GEC doc, join on doc headers to filter out non-binding status codes, then join each GEC-created
-- line on its related original source GLE (i.e. the GLE the line was built from). Finally insert a new GEC
-- Entry Relation record of each result. This should end up with the ENTRY_ID being a unique column, but
-- about 100 dirty records need to be deleted in the next step.
insert into FP_GEC_ENTRY_REL_T (ENTRY_ID, GEC_FDOC_NBR, GEC_FDOC_REF_NBR, GEC_FDOC_LN_TYP_CD, GEC_ACCT_LINE_NBR, GEC_ACCT_LINE_UUID, GEC_DOC_HDR_STAT_CD, VER_NBR, OBJ_ID)
select gle.ENTRY_ID, gecd.FDOC_NBR, lines.FDOC_REF_NBR, lines.FDOC_LN_TYP_CD, lines.FDOC_LINE_NBR, lines.OBJ_ID, hdr.DOC_HDR_STAT_CD, 1, SYS_GUID()
    from FP_ERROR_COR_DOC_T gecd
    inner join KREW_DOC_HDR_T hdr
        on gecd.FDOC_NBR = hdr.DOC_HDR_ID
        and hdr.DOC_HDR_STAT_CD in ('I', 'P', 'F', 'S', 'R')
    inner join FP_ACCT_LINES_T lines
        on gecd.FDOC_NBR = lines.FDOC_NBR
    inner join GL_ENTRY_T gle
        on gle.UNIV_FISCAL_YR in (
            (select to_char(sysdate, 'YYYY') from dual),
            (select to_char(sysdate, 'YYYY')-1 from dual)
        )
        and lines.FDOC_POST_YR     = gle.UNIV_FISCAL_YR
        and lines.FIN_COA_CD       = gle.FIN_COA_CD
        and lines.FDOC_REF_NBR     = gle.FDOC_NBR
        and lines.ACCOUNT_NBR      = gle.ACCOUNT_NBR
        and lines.FS_REF_ORIGIN_CD = gle.FS_ORIGIN_CD
        and lines.FIN_OBJECT_CD    = gle.FIN_OBJECT_CD
        and lines.FDOC_LINE_NBR    = gle.TRN_ENTR_SEQ_NBR
        and lines.FDOC_LINE_AMT    = gle.TRN_LDGR_ENTR_AMT
        and ((lines.FDOC_LN_TYP_CD = 'F' and gle.TRN_DEBIT_CRDT_CD = 'D')
            or
            (lines.FDOC_LN_TYP_CD = 'T' and gle.TRN_DEBIT_CRDT_CD = 'C'));


--------------------------------------------------------------------------------------------------------------------------
-- Cleanup inconsistent data where multiple GLEs appear to have been operated on by one GEC
--------------------------------------------------------------------------------------------------------------------------
-- Clean-up for inconsistent data, as viewed from ENTRY_ID; 94 rows as of 05-Jun-2017
-- Note: I'm using a temp table here as a memory and speed optimization
create table TEMP_T_SLDHEYHDNGL as
select ENTRY_ID
  from FP_GEC_ENTRY_REL_T
  where ENTRY_ID in (
    select ENTRY_ID
      from FP_GEC_ENTRY_REL_T
      group by ENTRY_ID
      having count(*) > 1
  );

-- Stamp the GLE, which will block the GLE from being used in GEC ever again
update GL_ENTRY_T gle
    set GEC_FDOC_NBR = 'LOCKED'
    where ENTRY_ID in (
      select ENTRY_ID
        from TEMP_T_SLDHEYHDNGL
    );

-- Clean up the relationships, since it's inconsistent to leave them
delete
    from FP_GEC_ENTRY_REL_T
    where ENTRY_ID in (
      select ENTRY_ID
        from TEMP_T_SLDHEYHDNGL
    );

-- Temp tables go away at the end of a session, but don't want it hanging
-- out for the rest of the upgrade
drop table TEMP_T_SLDHEYHDNGL;


--------------------------------------------------------------------------------------------------------------------------
-- Cleanup inconsistent data where one line appears to have been involved in multiple GECs
--------------------------------------------------------------------------------------------------------------------------
-- Clean-up for inconsistent data from AccountingLine's OBJ_ID
-- Roughly 92 rows, or 0.008% of the corpus, as of 05-Jun-2017

-- First create a temp table that have the multi-line issue, this was necessary
-- since a dynamic join breaks the prototype DB memory limits
create table TEMP_T_IUKSLJFYWBD as
select ENTRY_ID
  from FP_GEC_ENTRY_REL_T
  where GEC_ACCT_LINE_UUID in (
    select GEC_ACCT_LINE_UUID
      from FP_GEC_ENTRY_REL_T
      group by GEC_ACCT_LINE_UUID
      having count(*) > 1
  );

-- Now stamp the related GLE based on the temp table findings
update GL_ENTRY_T gle
  set GEC_FDOC_NBR = 'LOCKED'
  where ENTRY_ID in (
    select ENTRY_ID
      from TEMP_T_IUKSLJFYWBD
  );

-- Now delete the relationships, as the only records there should
-- be valid and active relationships; this will prevent a bad
-- relationship from being further operated on
delete
    from FP_GEC_ENTRY_REL_T
    where ENTRY_ID in (
      select ENTRY_ID
        from TEMP_T_IUKSLJFYWBD
    );

-- Cleanup, but this should go away at the close of the session, might as
-- well be explicit so that it's not hanging around the entire upgrade
drop table TEMP_T_IUKSLJFYWBD;

-- Now enforce the new UUID
create unique index ACCT_LN_UUID_IDX on FP_GEC_ENTRY_REL_T(GEC_ACCT_LINE_UUID);

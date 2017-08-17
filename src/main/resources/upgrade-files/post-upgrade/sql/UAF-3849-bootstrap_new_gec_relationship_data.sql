-- =====================================================================================================================
--  UAF-3849 : Bootstrap GEC Relationship Data
-- =====================================================================================================================
--     These changesets will create three new columns on the GL Entry table (GLE),
-- including a new object id for Rice to work with, a new GUID for each entry, and
-- a new GEC document number column. Further, these new columns will get appropriate
-- constraints and indexes added. Next, a new GEC Entry Relationship table is
-- created, then records added for existing GEC Doc -> Accounting Line -> GL Entry
-- relationships, with a minimal column count. The GEC Relationship table will similarly
-- have its own constraints/indexes/composite-PK applied.
--
-- There's several non-standard things going on here, so I'll point them out now:
--     1. We need to disable a BI trigger on the GLE table, since otherwise, each of
-- the 78MM records would update BI's reporting view, twice. Since we are not changing
-- financial information, and the new columns would not be in BI reports anyway, there
-- is no conflict in disabling the trigger for the duration of these changesets.
--     2. The new GLE sequence is based on the highest ID that's generated after the fact.
-- We don't really care if the sequence is interupted, it just turns out to be a nice
-- way of doing things, since this will work no matter the intial data set. To do it though,
-- we use PL/SQL, and the oracle built-in "ROWNUM". Liquibase has mechanisms to allow this,
-- but it does mean this script is RDBMS-specific to oracle.
--     3. These changesets represent large operations on 78MM+ rows -first updating, then
-- rolling back- I wanted to avoid spending cycles where we could. Since the main rollbacks
-- include dropping either columns or tables, then we really don't care about rolling back
-- the intermediate updates, constraints, or indexes; these will implicitly be deleted
-- when either a column is dropped, or the entire table is dropped. So to save time
-- and redo-log space, I stub-out several intermediate rollbacks with a no-op, which
-- tricks liquibase into thinking a particular rollback was performed and completed. The
-- end result is that the involved GLE table is restored to its original state, and all
-- other objects get deleted, either explicitly or implicitly so.
--     4. Note that rollbacks are executed in reverse order of each changeset's operations.
-- For insance, all 11 changesets' main SQL block is executed sequentially, and then each
-- rollback is applied in reverse sequence, 11 to 0.
-- Knowing this should make it clear why and where I decided to stub-out the rollbacks.


--------------------------------------------------------------------------------------------------------------------------
-- 01/12 - Disable BI syncing trigger
--------------------------------------------------------------------------------------------------------------------------
-- This trigger updates a view for helping BI decide if a particular record should be synced in the nightly BI snapshot.
-- We don't care about that during DB upgrade, only after the fact.
alter trigger BIUD_GL_ENTRY_T disable;


--------------------------------------------------------------------------------------------------------------------------
-- 02/12 - Create new columns in GLE table, subsequent changesets rely on these.
--------------------------------------------------------------------------------------------------------------------------
-- Note: multi-column alter is not part of the the ANSI/ISO SQL standard, this is specifc to Oracle dialect.
alter table GL_ENTRY_T
    add(ENTRY_ID NUMBER(19,0),
        GEC_FDOC_NBR VARCHAR2(36 BYTE),
        OBJ_ID VARCHAR2(36 BYTE));


--------------------------------------------------------------------------------------------------------------------------
-- 03/12 - Populate two of the new GLE columns with Oracle DB internal functions.
--------------------------------------------------------------------------------------------------------------------------
-- Note: this uses Oracle's proprietary 'ROWNUM' and SYS_GUID(), which are not standard ANSI/ISO SQL.
update GL_ENTRY_T
    set ENTRY_ID = ROWNUM,
        OBJ_ID = SYS_GUID();


--------------------------------------------------------------------------------------------------------------------------
-- 04/12 - Add not-null constraints and indexes on newly populated ENTRY_ID and OBJ_ID.
--------------------------------------------------------------------------------------------------------------------------
-- It is best practice to set explicit constraints, in case the index goes away
-- with its implicit contstraints, the others remains.
ALTER TABLE GL_ENTRY_T MODIFY (ENTRY_ID NOT NULL ENABLE);
CREATE UNIQUE INDEX GL_ENTRY_TI17 ON GL_ENTRY_T(ENTRY_ID);
ALTER TABLE GL_ENTRY_T MODIFY (OBJ_ID DEFAULT SYS_GUID() NOT NULL ENABLE);
CREATE UNIQUE INDEX GL_ENTRY_TI18 ON GL_ENTRY_T(OBJ_ID);


--------------------------------------------------------------------------------------------------------------------------
-- 05/12 - Create sequence for use on new GLE ENTRY_ID column.
--------------------------------------------------------------------------------------------------------------------------
-- Note: thefollowing code block is oracle-specific PL/SQL, where we need to use the end limiter '/'.
-- Actual function: select the highest ENTRY_ID value, and set the new sequence's starting point one up from that.
--
-- 2 GOTCHAs: OJDBC was parsing PL/SQL newlines wrong, and errantly detects EOF where there was none. So the stopgap,
--         is to remove all newlines in the PL/SQL (I'll refrain from ranting about the licensing fees we pay for
--         this level of quality).
--             The second bug is in App.java#getSqlStatements(...):503-507, where a trailing semicolon is stripped.
--         This is the general case, where JDBC doesn't want line terminators, but PL/SQL has a coinciding rule that its
--         blocks should be terminated by semicolon, which also happens to be at the end of a line. This is why there's
--         two ';' at the end of this statement: one will be consumed by App.java, and the other will be left for OJDBC.
--         This was tested in a modified kfsdbupgrade process, and observed to work correctly.
DECLARE new_start_id number; BEGIN select MAX(ENTRY_ID) + 1 into new_start_id from GL_ENTRY_T; execute immediate 'CREATE SEQUENCE GL_ENTRY_ID_SEQ START WITH ' || new_start_id || ' INCREMENT BY 1 NOMAXVALUE CACHE 500 NOCYCLE'; END;;


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
            (select UNIV_FISCAL_YR from SH_UNIV_DATE_T where UNIV_DT = trunc(sysdate)),
            (select (UNIV_FISCAL_YR - 1) from sh_univ_date_t where UNIV_DT = trunc(sysdate))
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
-- 08/12 - Create indexes on the new GEC Entry Relationship table.
--------------------------------------------------------------------------------------------------------------------------
CREATE INDEX FDOC_NBR_IDX ON FP_GEC_ENTRY_REL_T(GEC_FDOC_NBR);
CREATE INDEX ENTRY_ID_IDX ON FP_GEC_ENTRY_REL_T(ENTRY_ID);


--------------------------------------------------------------------------------------------------------------------------
-- 09/12 - Set constraints on the GEC Entry Relationship table, now that it's populated.
--------------------------------------------------------------------------------------------------------------------------
-- We should always have non-null for these; the composite PK is necessary for the lookup mechanism
-- in the framework, but also reflects data integrity. We do this now, since inserts and updates
-- will go faster without any contstraints.
ALTER TABLE FP_GEC_ENTRY_REL_T MODIFY (ENTRY_ID NOT NULL ENABLE);
ALTER TABLE FP_GEC_ENTRY_REL_T MODIFY (GEC_FDOC_NBR NOT NULL ENABLE);
ALTER TABLE FP_GEC_ENTRY_REL_T MODIFY (GEC_FDOC_LN_TYP_CD NOT NULL ENABLE);
ALTER TABLE FP_GEC_ENTRY_REL_T MODIFY (GEC_ACCT_LINE_NBR NOT NULL ENABLE);
ALTER TABLE FP_GEC_ENTRY_REL_T MODIFY (GEC_DOC_HDR_STAT_CD NOT NULL ENABLE);
ALTER TABLE FP_GEC_ENTRY_REL_T ADD CONSTRAINT GEC_ENTRY_RELATIONSHIP_TP1 PRIMARY KEY (ENTRY_ID, GEC_FDOC_NBR, GEC_FDOC_LN_TYP_CD, GEC_ACCT_LINE_NBR) ENABLE;


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


--------------------------------------------------------------------------------------------------------------------------
-- 12/12 - Associate existing Entries to their related GEC documents. This needs to be key-constrained, as in, the
-- relationship table should never have a GLE listed more than once.
--------------------------------------------------------------------------------------------------------------------------
-- Note: the following code block uses
-- an oracle specific join for update, which is the only valid syntax that oracle accepts
-- for a join within an update clause. Also, since we are not setting an FK relationship
-- here (don't want any overhead from checking the constraint), this method ensures there
-- is a 1:1 Entry ID relationship between the tables (ie, both tables are key-constrained).
update GL_ENTRY_T gle
  set GEC_FDOC_NBR =
    (select gec.GEC_FDOC_NBR
      from FP_GEC_ENTRY_REL_T gec
      where gle.ENTRY_ID = gec.ENTRY_ID)
  where exists
    (select 1
      from FP_GEC_ENTRY_REL_T gec
      where gle.ENTRY_ID = gec.ENTRY_ID
    );

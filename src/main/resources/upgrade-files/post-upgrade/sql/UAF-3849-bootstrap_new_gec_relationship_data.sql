-- =====================================================================================================================
--  UAF-3849 : Bootstrap GEC Relationship Data
-- =====================================================================================================================
--     These changesets will create three new columns on the GL Entry table (GLE),
-- including a new object id for Rice to work with, a new GUID for each entry, and
-- a new GEC document number column. Further, these new columns will get appropriate
-- constraints and indexes added. Also, a new GEC Entry Relationship table is
-- created for the KFSv7 GEC Mod to create records for GEC Doc -> Accounting Line -> GL Entry
-- relationships.
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
-- 01/08 - Disable BI syncing trigger
--------------------------------------------------------------------------------------------------------------------------
-- This trigger updates a view for helping BI decide if a particular record should be synced in the nightly BI snapshot.
-- We don't care about that during DB upgrade, only after the fact.
alter trigger BIUD_GL_ENTRY_T disable;


--------------------------------------------------------------------------------------------------------------------------
-- 02/08 - Create new columns in GLE table, subsequent changesets rely on these.
--------------------------------------------------------------------------------------------------------------------------
-- Note: multi-column alter is not part of the the ANSI/ISO SQL standard, this is specifc to Oracle dialect.
alter table GL_ENTRY_T
    add(ENTRY_ID NUMBER(19,0),
        GEC_FDOC_NBR VARCHAR2(36 BYTE),
        OBJ_ID VARCHAR2(36 BYTE));


--------------------------------------------------------------------------------------------------------------------------
-- 03/08 - Populate two of the new GLE columns with Oracle DB internal functions.
--------------------------------------------------------------------------------------------------------------------------
-- Note: this uses Oracle's proprietary 'ROWNUM' and SYS_GUID(), which are not standard ANSI/ISO SQL.
update GL_ENTRY_T
    set ENTRY_ID = ROWNUM,
        OBJ_ID = SYS_GUID();


--------------------------------------------------------------------------------------------------------------------------
-- 04/08 - Add not-null constraints and indexes on newly populated ENTRY_ID and OBJ_ID.
--------------------------------------------------------------------------------------------------------------------------
-- It is best practice to set explicit constraints, in case the index goes away
-- with its implicit contstraints, the others remains.
ALTER TABLE GL_ENTRY_T MODIFY (ENTRY_ID NOT NULL ENABLE);
CREATE UNIQUE INDEX GL_ENTRY_TI17 ON GL_ENTRY_T(ENTRY_ID);
ALTER TABLE GL_ENTRY_T MODIFY (OBJ_ID DEFAULT SYS_GUID() NOT NULL ENABLE);
CREATE UNIQUE INDEX GL_ENTRY_TI18 ON GL_ENTRY_T(OBJ_ID);


--------------------------------------------------------------------------------------------------------------------------
-- 05/08 - Create sequence for use on new GLE ENTRY_ID column.
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
-- 06/08 - Create new GEC Entry Relationship table. Note, the gec_fdoc_ref_nbr field is for backend work.
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
-- 07/08 - Set constraints on the GEC Entry Relationship table.
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
-- 08/08 - Create indexes on the new GEC Entry Relationship table.
--------------------------------------------------------------------------------------------------------------------------
CREATE INDEX FDOC_NBR_IDX ON FP_GEC_ENTRY_REL_T(GEC_FDOC_NBR);
CREATE INDEX ENTRY_ID_IDX ON FP_GEC_ENTRY_REL_T(ENTRY_ID);
CREATE UNIQUE INDEX ACCT_LN_UUID_IDX ON FP_GEC_ENTRY_REL_T(GEC_ACCT_LINE_UUID);

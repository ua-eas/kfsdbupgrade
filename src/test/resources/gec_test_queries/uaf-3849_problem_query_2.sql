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

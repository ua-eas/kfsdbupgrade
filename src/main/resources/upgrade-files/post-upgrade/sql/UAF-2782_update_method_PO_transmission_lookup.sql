-- =======================================================================================================================
-- (UAF-2782) : Update Method of PO Transmission Lookup Table. Subtask of (UAF-92)
-- =======================================================================================================================
UPDATE KULOWNER.PUR_PO_TRNS_MTHD_T SET DOBJ_MAINT_CD_ACTV_IND='Y' WHERE PO_TRNS_MTHD_CD='ELEC';
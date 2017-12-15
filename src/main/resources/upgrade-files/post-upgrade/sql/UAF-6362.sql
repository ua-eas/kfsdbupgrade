--------------------------------------------------------------------------------
--   UAF-6362 : Update ACH Transaction Type Maintenance Table (Subtask of UAF-92)
--------------------------------------------------------------------------------
delete from pdp_ach_trans_typ_t where trans_typ='TEM' and obj_id='KFS-ACH-TRANS-TYP-002' and ver_nbr=1 and trans_typ_desc='Travel and Entertainment Transaction Type';
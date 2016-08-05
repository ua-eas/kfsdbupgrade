-- ==============================================================================================
--  Inactivate permission 'Look Up Records KFS-GL TrialBalanceReport' that has conflicting roles
--  (UAF-2911) subtask of (UAF-92)
-- ==============================================================================================
update KRIM_PERM_T set ACTV_IND = 'N' where NM = 'Look Up Records KFS-GL TrialBalanceReport' and NMSPC_CD = 'KFS-GL';

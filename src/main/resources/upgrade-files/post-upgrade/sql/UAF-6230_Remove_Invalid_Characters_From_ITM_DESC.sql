-- =============================================================================================================
-- UAF-6230 Bug - Cannot approve enroute or new PREQ that contains special characters in the line description
-- KFS3 has a less restrictive validation pattern than KFS7 that allowed invalid characters to be inserted into the ITM_DESC field in the PUR_REQS_ITM_T, PUR_PO_ITM_T, and AP_PMT_RQST_ITM_T tables.
-- Instead of changing the more restrictive validation pattern in KFS7, this SQL cleans the KFS3 data and removes all invalid characters with ranges 128-255.
-- This will allow the documents that contain such invalid characters to be approved
-- =============================================================================================================
UPDATE AP_PMT_RQST_ITM_T K
  SET K.ITM_DESC = 
  (regexp_replace (K.ITM_DESC,'['
  ||CHR(128)
  ||'-'
  ||CHR(255)
  ||']','',1,0,'in'))
WHERE REGEXP_LIKE (K.ITM_DESC, '[' 
  || CHR(128) 
  || '-' 
  || CHR(255) 
  || ']', 'in');


UPDATE PUR_PO_ITM_T K
  SET K.ITM_DESC = 
  (regexp_replace (K.ITM_DESC,'['
  ||CHR(128)
  ||'-'
  ||CHR(255)
  ||']','',1,0,'in'))
WHERE REGEXP_LIKE (K.ITM_DESC, '[' 
  || CHR(128) 
  || '-' 
  || CHR(255) 
  || ']', 'in');

  
UPDATE PUR_REQS_ITM_T K
  SET K.ITM_DESC = 
  (regexp_replace (K.ITM_DESC,'['
  ||CHR(128)
  ||'-'
  ||CHR(255)
  ||']','',1,0,'in'))
WHERE REGEXP_LIKE (K.ITM_DESC, '[' 
  || CHR(128) 
  || '-' 
  || CHR(255) 
  || ']', 'in');

commit;

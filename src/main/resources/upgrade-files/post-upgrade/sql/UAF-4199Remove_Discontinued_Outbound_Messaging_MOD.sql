--=======================================================================================================================================
-- UAF-4199 : Removal of Doc Type, Parameters, Parm Components & Permission From Discontinued Outbound Messaging MOD. Subtask of (UAF-92)
--=======================================================================================================================================
----------------- DELETE PARAMETERS --------------------------------------------
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageBusinessObjectPayload' AND PARM_NM = 'OUTBOUND_MESSAGING_ENDPOINT_URL';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageQueue' AND PARM_NM = 'OUTBOUND_MESSAGING_POLL_FREQUENCY_SECONDS';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageQueue' AND PARM_NM = 'OUTBOUND_MESSAGING_QUEUE_MAX_AGE_DAYS';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageQueue' AND PARM_NM = 'OUTBOUND_MESSAGING_QUEUE_MAX_SEND_ATTEMPTS';
DELETE FROM KULOWNER.KRCR_PARM_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageQueue' AND PARM_NM = 'ENABLE_OUTBOUND_MESSAGING_IND';
----------------- DELETE PARAMETER COMPONENTS ----------------------------------
DELETE FROM KULOWNER.KRCR_CMPNT_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageBusinessObjectPayload';
DELETE FROM KULOWNER.KRCR_CMPNT_T WHERE NMSPC_CD = 'KFS-SYS' AND CMPNT_CD = 'OutboundMessageQueue';
----------------- DELETE DOCUMENT TYPES ----------------------------------------
DELETE FROM KULOWNER.KREW_DOC_TYP_T WHERE DOC_TYP_NM = 'OMQI' AND LBL = 'Outbound Message Queue Item Maintenance';
----------------- DELETE PERMISSION ATTRIBUTES ---------------------------------
DELETE FROM KULOWNER.KRIM_PERM_ATTR_DATA_T WHERE PERM_ID = '10495';
----------------- DELETE ROLE PERMISSIONS --------------------------------------
DELETE FROM KULOWNER.KRIM_ROLE_PERM_T WHERE PERM_ID = '10495';
----------------- DELETE PERMISSIONS -------------------------------------------
DELETE FROM KULOWNER.KRIM_PERM_T WHERE PERM_ID = '10495' AND NMSPC_CD = 'KFS-SYS' AND NM = 'Manually Send Message sendButton';

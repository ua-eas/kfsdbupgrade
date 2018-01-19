-- ============================================================================
-- UAF-6590 : Convert GTED (Global Transaction Edit Detail). SubTask of UAF-3045
-- ============================================================================
UPDATE KREW_DOC_TYP_T SET DOC_HDLR_URL = '' WHERE DOC_TYP_NM = 'GTED' AND DOC_TYP_ID = '337702' AND PARNT_ID = '320509';
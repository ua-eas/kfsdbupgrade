-- ============================================================================
-- Update Document Type from old 'PCDH' to new type 'PCDM'
-- ============================================================================
update krew_doc_hdr_t set doc_typ_id=(select doc_typ_id from KREW_DOC_TYP_T where DOC_TYP_NM='PCDM') where doc_typ_id = '344984';
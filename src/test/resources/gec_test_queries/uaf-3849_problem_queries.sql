-- Originally the string [LOCKED] had double quotes, single passes
update GL_ENTRY_T gle
    set GEC_FDOC_NBR = 'LOCKED'
    where ENTRY_ID in
        (select
            ENTRY_ID
            from FP_GEC_ENTRY_REL_T
            group by ENTRY_ID
            having count(*) > 1
        );

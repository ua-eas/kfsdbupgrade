alter table KRLC_CNTRY_T add ALT_POSTAL_CNTRY_CD VARCHAR2(3);
update krim_rsp_t set ver_nbr = 1 where ver_nbr = 0;

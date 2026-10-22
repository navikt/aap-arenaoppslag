-- Dagsats med barnetillegg før samordning for vedtak 90010. Verdien skiller seg fra DAGSMBT (550)
-- slik at testene kan se at feltene hentes fra hver sin vedtakfakta. Vedtak 90040 får ikke DAGSMBFSAM.
insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI, REG_DATO)
values (90010, 'DAGSMBFSAM', '600', DATE '2023-01-01');


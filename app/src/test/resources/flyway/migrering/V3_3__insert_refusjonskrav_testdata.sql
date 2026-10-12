-- Testdata for refusjonskrav-endepunktet i migrering.
-- Saksnummer 2023-505: siste løpende aap-vedtak med refusjonskrav (UTBETVENT*).
-- Saksnummer 2023-506: siste løpende aap-vedtak uten refusjonskrav.
-- Datoene er relative til CURRENT_DATE, så vedtakene alltid dekker dagens dato.

insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (204, '20400000000', 'Refusjonskrav', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9105, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 204, 2023, 505, null, 'AKTIV',
        '4402', null, 'N'),
       (9106, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 204, 2023, 506, null, 'AKTIV',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91051, 9105, 'IVERK', 'O', 'JA', 'AAP', 204, DATEADD('DAY', -30, CURRENT_DATE), null, '4402', 505, 2023, 1,
        'UA', DATEADD('DAY', -30, CURRENT_DATE)),
       (91061, 9106, 'IVERK', 'O', 'JA', 'AAP', 204, DATEADD('DAY', -30, CURRENT_DATE), null, '4402', 506, 2023, 1,
        'UA', DATEADD('DAY', -30, CURRENT_DATE));

insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI, REG_DATO)
values (91051, 'UTBETVENTK', 'REFKRAVSOS', DATE '2024-01-15'),
       (91051, 'UTBETVENTF', '01-02-2024', DATE '2024-01-15'),
       (91051, 'UTBETVENTT', '31-03-2024', DATE '2024-01-15'),
       -- Annet vedtaksfakta på samme vedtak skal ignoreres
       (91051, 'UTBETVENTB', 'Refusjonskrav sosialhjelp', DATE '2024-01-15');

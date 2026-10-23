-- Testdata for beregningstidspunkt-endepunktet i migrering.
-- Saksnummer 2023-507: siste løpende aap-vedtak med AAPBERDATO.
-- Saksnummer 2023-508: siste løpende aap-vedtak uten AAPBERDATO.
-- Datoene er relative til CURRENT_DATE, så vedtakene alltid dekker dagens dato.

insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (205, '20500000000', 'Beregningstidspunkt', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9301, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 205, 2023, 507, null, 'AKTIV',
        '4402', null, 'N'),
       (9302, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 205, 2023, 508, null, 'AKTIV',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (93011, 9301, 'IVERK', 'O', 'JA', 'AAP', 205, DATEADD('DAY', -30, CURRENT_DATE), null, '4402', 507, 2023, 1,
        'UA', DATEADD('DAY', -30, CURRENT_DATE)),
       (93021, 9302, 'IVERK', 'O', 'JA', 'AAP', 205, DATEADD('DAY', -30, CURRENT_DATE), null, '4402', 508, 2023, 1,
        'UA', DATEADD('DAY', -30, CURRENT_DATE));

insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI, REG_DATO)
values (93011, 'AAPBERDATO', '15-03-2024', DATE '2024-01-15'),
       -- Annet vedtaksfakta på samme vedtak skal ignoreres
       (93011, 'AAPBERREGL', 'REGELVERK1', DATE '2024-01-15');

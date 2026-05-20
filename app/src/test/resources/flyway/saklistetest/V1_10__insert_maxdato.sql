-- Testdata for spørringen som henter nyeste vedtak + maxdato per sak per person.
-- Brukes både av SakRepositoryTest og av MaksdatoApiTest.


-- =========================================================================
-- Testdata for `hent maksdato paa saker for person finner forventede data`
-- Person 100 har to saker (1102, 1103) som skal returneres,
-- pluss diverse støy som skal filtreres bort av spørringen.
-- =========================================================================

insert into PERSON (PERSON_ID, FODSELSNR, FORNAVN, ETTERNAVN)
values (100, 'maksdato100', 'Maks', 'Datosen'),
       -- annen person med tilsvarende data — skal IKKE komme med pga person_id-filter
       (101, 'annen101', 'Annen', 'Person');

-- Sak 1102 — flere vedtak
-- Saken er avsluttet (sakstatuskode != 'AKTIV') for å verifisere at AKTIV-kravet er fjernet.
Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR, LOPENRSAK,
                 DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, ER_UTLAND)
values (1102, 'AA', DATE '2022-02-02', DATE '2022-02-02', 'TEST', 'PERS', 100, 2022, 1102,
        DATE '2024-06-15', 'AVSLU', '0826', 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID,
                    FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK, AKTFASEKODE, DATO_MOTTATT)
values
    (1101, 1102, 'IVERK', 'O', 'JA', 'AAP', 100, DATE '2010-08-29', DATE '2026-06-30',
     '4402', 1102, 2022, 1, 'IKKE', DATE '2010-08-29'),
    (1100, 1102, 'IVERK', 'O', 'JA', 'AAP', 100, DATE '2009-01-01', DATE '2025-05-30',
     '4402', 1102, 2022, 0, 'IKKE', DATE '2009-01-01'),
    -- stanset vedtak
    (1109, 1102, 'IVERK', 'S', 'JA', 'AAP', 100, DATE '2024-01-01', DATE '2024-12-31',
     '4402', 1102, 2022, 9, 'IKKE', DATE '2024-01-01');

-- Sak 1103 — ett relevant vedtak (1103) + ett ikke-AAP-vedtak som skal filtreres bort
Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR, LOPENRSAK,
                 DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, ER_UTLAND)
values (1103, 'AA', DATE '2022-02-03', DATE '2022-02-03', 'TEST', 'PERS', 100, 2022, 1103, null, 'AKTIV', '0826', 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID,
                    FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK, AKTFASEKODE, DATO_MOTTATT)
values (1103, 1103, 'IVERK', 'O', 'JA', 'AAP', 100, DATE '2022-08-30', DATE '2025-12-31',
        '4402', 1103, 2022, 1, 'IKKE', DATE '2022-08-30'),
       -- skal filtreres bort: feil rettighetkode
       (1108, 1103, 'IVERK', 'O', 'JA', 'OOP', 100, DATE '2023-01-01', DATE '2024-01-01',
        '4402', 1103, 2022, 8, 'IKKE', DATE '2023-01-01');

-- Sak + vedtak for person 101 — skal filtreres bort av person_id = ?
Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR, LOPENRSAK,
                 DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, ER_UTLAND)
values (1110, 'AA', DATE '2022-02-10', DATE '2022-02-10', 'TEST', 'PERS', 101, 2022, 1110, null, 'AKTIV', '0826', 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID,
                    FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK, AKTFASEKODE, DATO_MOTTATT)
values (1111, 1110, 'IVERK', 'O', 'JA', 'AAP115', 101, DATE '2020-01-01', DATE '2030-01-01',
        '4402', 1110, 2022, 1, 'IKKE', DATE '2020-01-01');

insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI)
values
    -- person 100, sak 1102
    (1101, 'AAPVILKUNN', '30-06-2025'),
    (1100, 'AAPVILKUNN', '30-05-2025'),
    (1109, 'AAPVILKUNN', '31-12-2024'),
    -- person 100, sak 1103
    (1103, 'AAPVILKUNN', '30-06-2025'),
    (1108, 'AAPVILKUNN', '30-06-2025'),
    -- person 101 — skal ikke komme med
    (1111, 'AAPVILKUNN', '30-06-2025');

insert into V_VEDTAK_MAXDATO (VEDTAK_ID, MAX_DATO, MAX_UNNTAK_DATO)
values
    -- person 100, sak 1102 (maks er i vedtak 1101)
    (1101, DATE '2025-06-30', DATE '2026-06-30'),
    (1100, DATE '2025-05-30', DATE '2026-05-30'),
    (1109, DATE '2024-12-31', DATE '2025-06-30'),
    -- person 100, sak 1103
    (1103, DATE '2025-06-30', DATE '2025-12-31'),
    (1108, DATE '2024-12-31', DATE '2025-12-31'),
    -- person 101
    (1111, DATE '2030-01-01', DATE '2031-01-01');

-- Testdata for MaksimumRepositoryTest
-- Person med ett vedtak, to meldekortperioder med utbetalinger og meldekortdata

-- Person med gyldig vedtak
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (100, '12345678901', 'Testesen', 'Maksimum');

-- Sak
Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9001, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 100, 2023, 9001, null, 'INAKT',
        '4402', null, 'N');

-- Vedtak (gyldig: utfallkode=JA, rettighetkode=AAP, vedtaktypekode=O, vedtakstatuskode=IVERK)
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (90010, 9001, 'IVERK', 'O', 'JA', 'AAP', 100,
        DATE '2023-01-01', DATE '2023-12-31', '4402', 9001, 2023, 1, 'IKKE', DATE '2023-01-01');

-- VedtakFakta for vedtak 90010
insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI)
values (90010, 'DAGSMBT', '550'),
       (90010, 'BARNTILL', '30'),
       (90010, 'DAGS', '520'),
       (90010, 'BARNMSTON', '2'),
       (90010, 'DAGSFSAM', '520'),
       (90010, 'GRUNN', '450000'),
       (90010, 'JUSTERTG', 'NyG2024');

-- Meldekortperioder
insert into MELDEKORTPERIODE (AAR, PERIODEKODE, UKENR_UKE1, UKENR_UKE2, DATO_FRA, DATO_TIL)
values (2023, '01', 1, 2, DATE '2023-01-02', DATE '2023-01-15'),
       (2023, '02', 3, 4, DATE '2023-01-16', DATE '2023-01-29');

-- Meldekort 1 (periode 01/2023)
insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE)
values (5001, 100, 2023, '01', 'E1', 'FERDI');

-- Meldekort 2 (periode 02/2023)
insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE)
values (5002, 100, 2023, '02', 'E1', 'FERDI');

-- Meldekortdager for meldekort 5001: 3 + 2 = 5 timer arbeidet
insert into MELDEKORTDAG (MELDEKORT_ID, UKENR, DAGNR, STATUS_ARBEIDSDAG, STATUS_KURS, STATUS_SYK, TIMER_ARBEIDET)
values (5001, 1, 1, 'J', 'N', 'N', 3.0),
       (5001, 1, 2, 'J', 'N', 'N', 2.0),
       (5001, 1, 3, 'N', 'N', 'N', 0.0),
       (5001, 1, 4, 'N', 'N', 'N', 0.0),
       (5001, 1, 5, 'N', 'N', 'N', 0.0),
       (5001, 2, 1, 'N', 'N', 'N', 0.0),
       (5001, 2, 2, 'N', 'N', 'N', 0.0),
       (5001, 2, 3, 'N', 'N', 'N', 0.0),
       (5001, 2, 4, 'N', 'N', 'N', 0.0),
       (5001, 2, 5, 'N', 'N', 'N', 0.0);

-- Meldekortdager for meldekort 5002: 4 timer arbeidet
insert into MELDEKORTDAG (MELDEKORT_ID, UKENR, DAGNR, STATUS_ARBEIDSDAG, STATUS_KURS, STATUS_SYK, TIMER_ARBEIDET)
values (5002, 3, 1, 'J', 'N', 'N', 4.0),
       (5002, 3, 2, 'N', 'N', 'N', 0.0),
       (5002, 3, 3, 'N', 'N', 'N', 0.0),
       (5002, 3, 4, 'N', 'N', 'N', 0.0),
       (5002, 3, 5, 'N', 'N', 'N', 0.0),
       (5002, 4, 1, 'N', 'N', 'N', 0.0),
       (5002, 4, 2, 'N', 'N', 'N', 0.0),
       (5002, 4, 3, 'N', 'N', 'N', 0.0),
       (5002, 4, 4, 'N', 'N', 'N', 0.0),
       (5002, 4, 5, 'N', 'N', 'N', 0.0);

-- Posteringer knyttet til vedtak 90010 og meldekortene
insert into POSTERING (POSTERING_ID, BELOP, BELOPKODE, DATO_PERIODE_FRA, DATO_PERIODE_TIL, DATO_POSTERT, AAR,
                       PERSON_ID, POSTERINGTYPEKODE, TRANSAKSJONSKODE, DATO_GRUNNLAG, VEDTAK_ID, ARTKODE,
                       KAPITTEL, POST, UNDERPOST, BRUKER_ID_SAKSBEHANDLER, AETATENHET_ANSVARLIG, MELDEKORT_ID)
values (8001, 7700, 'AAP', DATE '2023-01-02', DATE '2023-01-15', DATE '2023-01-20', 2023, 100,
        'ORD', 'AA00', DATE '2023-01-02', 90010, 'ART', '2900', '01', '001', 'TEST', '4402', 5001),
       (8002, 6600, 'AAP', DATE '2023-01-16', DATE '2023-01-29', DATE '2023-02-03', 2023, 100,
        'ORD', 'AA00', DATE '2023-01-16', 90010, 'ART', '2900', '01', '001', 'TEST', '4402', 5002);

-- Meldekortdata for meldekort 5001: 1 sykedag (FSNN)
insert into ANMERKNING (ANMERKNING_ID, ANMERKNINGKODE, TABELLNAVNALIAS, OBJEKT_ID, VEDTAK_ID, VERDI)
values (7001, 'FSNN', 'MKORT', 5001, 90010, 1);

-- Meldekortdata for meldekort 5002: for sent (SENN=1) og fravær (FXNN=2)
insert into ANMERKNING (ANMERKNING_ID, ANMERKNINGKODE, TABELLNAVNALIAS, OBJEKT_ID, VEDTAK_ID, VERDI)
values (7002, 'SENN', 'MKORT', 5002, 90010, 1),
       (7003, 'FXNN', 'MKORT', 5002, 90010, 2);

-- Person uten vedtak
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (101, '00000000000', 'Vedtaksløs', 'Inga');

-- Person med vedtak utenfor søkeperioden
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (102, '11111111111', 'Utenfor', 'Periode');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9002, 'AA', DATE '2020-01-01', 'TEST', DATE '2020-01-01', 'TEST', 'PERS', 102, 2020, 9002, null, 'INAKT',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (90020, 9002, 'IVERK', 'O', 'JA', 'AAP', 102,
        DATE '2020-01-01', DATE '2020-12-31', '4402', 9002, 2020, 1, 'IKKE', DATE '2020-01-01');

-- Person med åpent vedtak (til_dato = NULL) — brukes for å reprodusere duplikat-bug
-- Bug: JOIN på MELDEKORTPERIODE mangler AND mkp.aar = m.aar, noe som gir én rad per år
-- med samme periodekode. Med NULL til_dato blir søkevinduet stort nok til at flere
-- år treffer datofilteret, og vi får duplikate utbetalingsrader.
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (103, '22222222222', 'Åpen', 'Vedtak');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9003, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 103, 2023, 9003, null, 'INAKT',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (90030, 9003, 'IVERK', 'O', 'JA', 'AAP', 103,
        DATE '2023-01-01', NULL, '4402', 9003, 2023, 1, 'IKKE', DATE '2023-01-01');

insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI)
values (90030, 'DAGSMBT', '500'),
       (90030, 'BARNTILL', '0'),
       (90030, 'DAGS', '500'),
       (90030, 'BARNMSTON', '0'),
       (90030, 'DAGSFSAM', '500'),
       (90030, 'GRUNN', '400000'),
       (90030, 'JUSTERTG', 'NyG2024');

-- Meldekortperiode for 2023, periode 10 — finnes også for 2024 (samme periodekode, forskjellig år)
-- Dette er det som trigger duplikater: JOIN uten AAR-betingelse treffer begge radene
insert into MELDEKORTPERIODE (AAR, PERIODEKODE, UKENR_UKE1, UKENR_UKE2, DATO_FRA, DATO_TIL)
values (2023, '10', 19, 20, DATE '2023-05-08', DATE '2023-05-21'),
       (2024, '10', 19, 20, DATE '2024-05-06', DATE '2024-05-19');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE)
values (6001, 103, 2023, '10', 'E1', 'FERDI');

insert into MELDEKORTDAG (MELDEKORT_ID, UKENR, DAGNR, STATUS_ARBEIDSDAG, STATUS_KURS, STATUS_SYK, TIMER_ARBEIDET)
values (6001, 19, 1, 'J', 'N', 'N', 3.0),
       (6001, 19, 2, 'N', 'N', 'N', 0.0),
       (6001, 19, 3, 'N', 'N', 'N', 0.0),
       (6001, 19, 4, 'N', 'N', 'N', 0.0),
       (6001, 19, 5, 'N', 'N', 'N', 0.0),
       (6001, 20, 1, 'N', 'N', 'N', 0.0),
       (6001, 20, 2, 'N', 'N', 'N', 0.0),
       (6001, 20, 3, 'N', 'N', 'N', 0.0),
       (6001, 20, 4, 'N', 'N', 'N', 0.0),
       (6001, 20, 5, 'N', 'N', 'N', 0.0);

insert into POSTERING (POSTERING_ID, BELOP, BELOPKODE, DATO_PERIODE_FRA, DATO_PERIODE_TIL, DATO_POSTERT, AAR,
                       PERSON_ID, POSTERINGTYPEKODE, TRANSAKSJONSKODE, DATO_GRUNNLAG, VEDTAK_ID, ARTKODE,
                       KAPITTEL, POST, UNDERPOST, BRUKER_ID_SAKSBEHANDLER, AETATENHET_ANSVARLIG, MELDEKORT_ID)
values (9001, 5000, 'AAP', DATE '2023-05-08', DATE '2023-05-21', DATE '2023-05-25', 2023, 103,
        'ORD', 'AA00', DATE '2023-05-08', 90030, 'ART', '2900', '01', '001', 'TEST', '4402', 6001);

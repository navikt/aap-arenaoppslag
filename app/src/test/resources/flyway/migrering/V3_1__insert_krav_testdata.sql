-- Testdata for MigreringApiTest / KravService-endepunktet.
-- Meldekortperiode og kvotebruk-datoer er relative til CURRENT_DATE, slik at "gjeldende periode"
-- alltid dekker dagens dato uavhengig av når testene kjøres.

insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (200, '20000000000', 'Migrant', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9101, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 200, 2023, 501, null, 'AKTIV',
        '4402', null, 'N');

-- Avslag før innvilgelsen — skal ikke telle som søknadsdato
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91011, 9101, 'AVSLU', 'O', 'NEI', 'AAP', 200,
        DATE '2023-06-01', DATE '2023-06-01', '4402', 501, 2023, 1, 'IKKE', DATE '2023-06-01');

-- Første innvilgede vedtak — dette er søknadsdatoen som skal returneres
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91012, 9101, 'IVERK', 'O', 'JA', 'AAP', 200,
        DATE '2023-07-01', DATE '2024-06-30', '4402', 501, 2023, 2, 'IKKE', DATE '2023-07-01');

-- Senere innvilget vedtak — skal ikke overstyre det første
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91013, 9101, 'IVERK', 'E', 'JA', 'AAP', 200,
        DATE '2023-09-01', null, '4402', 501, 2023, 3, 'IKKE', DATE '2023-09-01');

-- Meldekortperiode som dekker dagens dato uansett når testen kjøres
insert into MELDEKORTPERIODE (AAR, PERIODEKODE, UKENR_UKE1, UKENR_UKE2, DATO_FRA, DATO_TIL)
values (9999, '99', 1, 2, DATEADD('DAY', -7, CURRENT_DATE), DATEADD('DAY', 7, CURRENT_DATE));

-- Kvotebevegelser for person 200:
-- INIT (60 dager før i dag) gir saldo 260 (full ordinær kvote), en bevegelse (20 dager før i dag,
-- altså før migreringsdatoen som er 7 dager før i dag) trekker ned til 150, og en bevegelse etter
-- migreringsdatoen (3 dager fram i tid) trekker videre til 147 — denne siste skal IKKE telle med
-- i saldoen på migreringstidspunktet.
insert into KVOTEBRUK (KVOTEBRUK_ID, KVOTETYPEKODE, TABELLNAVNALIAS_GRUNNLAG, OBJEKT_ID_GRUNNLAG, ANTALL_BEVEGELSE,
                       POSTERINGTYPEKODE, REG_USER, REG_DATO, DATO_HENDELSE, PERSON_ID, BEGRUNNELSE)
values (300, 'AAP', 'VEDTAK', 91012, 260, 'INIT', 'TEST', DATEADD('DAY', -60, CURRENT_DATE),
        DATEADD('DAY', -60, CURRENT_DATE), 200, 'Innvilget'),
       (301, 'AAP', 'MKORT', 1, -110, 'OPPD', 'TEST', DATEADD('DAY', -20, CURRENT_DATE),
        DATEADD('DAY', -20, CURRENT_DATE), 200, 'Meldekort'),
       (302, 'AAP', 'MKORT', 2, -3, 'OPPD', 'TEST', DATEADD('DAY', 3, CURRENT_DATE),
        DATEADD('DAY', 3, CURRENT_DATE), 200, 'Meldekort');

-- Sak uten noe innvilget vedtak — brukes for å verifisere at søknadsdato og kvote blir null
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (201, '20100000000', 'Uinnvilget', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9102, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 201, 2023, 502, null, 'AKTIV',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91021, 9102, 'AVSLU', 'O', 'NEI', 'AAP', 201,
        DATE '2023-06-01', DATE '2023-06-01', '4402', 502, 2023, 1, 'IKKE', DATE '2023-06-01');

-- Sak med lengre vedtaks- og kvotehistorikk, der kvoten er brukt opp innen migreringstidspunktet
-- (saksnummer "2023-503"). Nyttig for å manuelt verifisere at gjenstaaendeOrdinaerKvote kan bli 0,
-- og at flere meldekorttrekk over tid akkumuleres riktig.
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (202, '20200000000', 'Kvoteoppbrukt', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9103, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 202, 2023, 503, null, 'AKTIV',
        '4402', null, 'N');

-- Avslag før innvilgelsen — skal ikke telle som søknadsdato
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91031, 9103, 'AVSLU', 'O', 'NEI', 'AAP', 202,
        DATE '2023-01-01', DATE '2023-01-01', '4402', 503, 2023, 1, 'IKKE', DATE '2023-01-01');

-- Første innvilgede vedtak — dette er søknadsdatoen som skal returneres
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91032, 9103, 'IVERK', 'O', 'JA', 'AAP', 202,
        DATE '2023-02-01', DATE '2024-01-31', '4402', 503, 2023, 2, 'IKKE', DATE '2023-02-01');

-- Revurdering — nytt innvilget vedtak, skal ikke overstyre søknadsdatoen fra det første
insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT)
values (91033, 9103, 'IVERK', 'E', 'JA', 'AAP', 202,
        DATE '2023-08-01', null, '4402', 503, 2023, 3, 'IKKE', DATE '2023-08-01');

-- Kvotebevegelser for person 202: startsaldo 20, fire meldekorttrekk à 5 dager fram til migreringsdatoen
-- (7 dager før i dag) bruker opp hele kvoten (20 -> 15 -> 10 -> 5 -> 0). En bevegelse etter
-- migreringsdatoen skal ikke telle med — saldoen skal fortsatt vise 0, ikke negativ.
insert into KVOTEBRUK (KVOTEBRUK_ID, KVOTETYPEKODE, TABELLNAVNALIAS_GRUNNLAG, OBJEKT_ID_GRUNNLAG, ANTALL_BEVEGELSE,
                       POSTERINGTYPEKODE, REG_USER, REG_DATO, DATO_HENDELSE, PERSON_ID, BEGRUNNELSE)
values (310, 'AAP', 'VEDTAK', 91032, 20, 'INIT', 'TEST', DATEADD('DAY', -300, CURRENT_DATE),
        DATEADD('DAY', -300, CURRENT_DATE), 202, 'Innvilget'),
       (311, 'AAP', 'MKORT', 11, -5, 'OPPD', 'TEST', DATEADD('DAY', -200, CURRENT_DATE),
        DATEADD('DAY', -200, CURRENT_DATE), 202, 'Meldekort'),
       (312, 'AAP', 'MKORT', 12, -5, 'OPPD', 'TEST', DATEADD('DAY', -100, CURRENT_DATE),
        DATEADD('DAY', -100, CURRENT_DATE), 202, 'Meldekort'),
       (313, 'AAP', 'MKORT', 13, -5, 'OPPD', 'TEST', DATEADD('DAY', -30, CURRENT_DATE),
        DATEADD('DAY', -30, CURRENT_DATE), 202, 'Meldekort'),
       (314, 'AAP', 'MKORT', 14, -5, 'OPPD', 'TEST', DATEADD('DAY', -10, CURRENT_DATE),
        DATEADD('DAY', -10, CURRENT_DATE), 202, 'Meldekort'),
       (315, 'AAP', 'MKORT', 15, -2, 'OPPD', 'TEST', DATEADD('DAY', 5, CURRENT_DATE),
        DATEADD('DAY', 5, CURRENT_DATE), 202, 'Meldekort etter migreringsdato');

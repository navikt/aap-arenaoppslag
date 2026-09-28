-- Kvotebevegelser for person 100, knyttet til meldekortene 5001 og 5002.
-- INIT-radene setter startsaldo per kvotetype, MKORT-radene trekker kvote for hvert meldekort.
-- Saldoen (resterende) akkumuleres per kvotetype i stigende KVOTEBRUK_ID-rekkefølge:
--   AAP:   20 -> 10 (meldekort 5001) -> 4 (meldekort 5002)
--   MAAPU: 30 -> 30 (ikke trukket for 5001) -> 25 (meldekort 5002)
Insert into KVOTEBRUK (KVOTEBRUK_ID, KVOTETYPEKODE, TABELLNAVNALIAS_GRUNNLAG, OBJEKT_ID_GRUNNLAG, ANTALL_BEVEGELSE,
                       POSTERINGTYPEKODE, REG_USER, REG_DATO, DATO_HENDELSE, PERSON_ID, BEGRUNNELSE)
values (200, 'AAP', 'VEDTAK', 90010, 20, 'INIT', 'TEST', DATE '2023-01-01', DATE '2023-01-01', 100, 'Innvilget'),
       (201, 'MAAPU', 'VEDTAK', 90010, 30, 'INIT', 'TEST', DATE '2023-01-01', DATE '2023-01-01', 100, 'Unntak'),
       (202, 'AAP', 'MKORT', 5001, -10, 'OPPD', 'TEST', DATE '2023-01-20', DATE '2023-01-20', 100, 'Meldekort'),
       (203, 'AAP', 'MKORT', 5002, -6, 'OPPD', 'TEST', DATE '2023-02-03', DATE '2023-02-03', 100, 'Meldekort'),
       (204, 'MAAPU', 'MKORT', 5002, -5, 'OPPD', 'TEST', DATE '2023-02-03', DATE '2023-02-03', 100, 'Meldekort');

-- BEREGNINGSLEDD holder gjeldende saldo per kvotetype for personen (brukes av telleverk-oppslaget)
Insert into BEREGNINGSLEDD (BEREGNINGSLEDD_ID, BEREGNINGSLEDDKODE, DATO_FRA, DATO_TIL, TABELLNAVNALIAS_KILDE,
                            OBJEKT_ID_KILDE, PERSON_ID, VERDI)
values (200, 'AAP', DATE '2023-01-01', DATE '2023-12-31', 'KVOTBR', 203, 100, 4),
       (201, 'MAAPU', DATE '2023-01-01', DATE '2023-12-31', 'KVOTBR', 204, 100, 25);


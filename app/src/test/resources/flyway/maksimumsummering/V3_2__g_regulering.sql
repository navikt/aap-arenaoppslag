INSERT INTO person (person_id, fodselsnr, etternavn, fornavn)
VALUES (122, '12200000000', 'Regulering', 'Test');

INSERT INTO sak (sak_id, sakskode, reg_dato, reg_user, mod_dato, mod_user, tabellnavnalias, objekt_id,
                 aar, lopenrsak, sakstatuskode, aetatenhet_ansvarlig, er_utland)
VALUES (9203, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 122,
        2023, 9203, 'INAKT', '4402', 'N');

INSERT INTO vedtak (vedtak_id, sak_id, vedtakstatuskode, vedtaktypekode, utfallkode, rettighetkode,
                    person_id, fra_dato, til_dato, aetatenhet_behandler, lopenrsak, aar, lopenrvedtak,
                    aktfasekode, dato_mottatt)
VALUES (92030, 9203, 'IVERK', 'O', 'JA', 'AAP', 122,
        DATE '2023-02-06', DATE '2023-02-05', '4402', 9203, 2023, 1, 'IKKE', DATE '2023-01-01'),
       (92031, 9203, 'IVERK', 'O', 'JA', 'AAP', 122,
        DATE '2023-01-01', DATE '2023-02-05', '4402', 9203, 2023, 2, 'IKKE', DATE '2023-01-01'),
       (92032, 9203, 'IVERK', 'E', 'JA', 'AAP', 122,
        DATE '2023-02-06', NULL, '4402', 9203, 2023, 3, 'IKKE', DATE '2023-02-06'),
       (92033, 9203, 'IVERK', 'O', 'JA', 'DAGO', 122,
        DATE '2023-02-06', DATE '2023-02-05', '4402', 9203, 2023, 4, 'IKKE', DATE '2023-01-01');

INSERT INTO vedtakfakta (vedtak_id, vedtakfaktakode, vedtakverdi, reg_dato)
VALUES (92031, 'DAGSMBT', '600', DATE '2023-01-01'),
       (92031, 'BARNTILL', '20', DATE '2023-01-01'),
       (92031, 'DAGSFSAM', '580', DATE '2023-01-01'),
       (92032, 'DAGSMBT', '650', DATE '2023-02-06'),
       (92032, 'BARNTILL', '30', DATE '2023-02-06'),
       (92032, 'DAGSFSAM', '620', DATE '2023-02-06');

INSERT INTO meldekortperiode (aar, periodekode, ukenr_uke1, ukenr_uke2, dato_fra, dato_til)
VALUES (2023, '05', 5, 6, DATE '2023-01-30', DATE '2023-02-12');

INSERT INTO meldekort (meldekort_id, person_id, aar, periodekode, mkskortkode, beregningstatuskode)
VALUES (7504, 122, 2023, '05', 'E1', 'FERDI');

INSERT INTO meldekortdag (meldekort_id, ukenr, dagnr, status_arbeidsdag, status_kurs, status_syk, timer_arbeidet)
VALUES (7504, 5, 1, 'J', 'N', 'N', 7.5),
       (7504, 6, 1, 'N', 'N', 'N', 0),
       (7504, 6, 7, 'J', 'N', 'N', 2.5);

INSERT INTO postering (postering_id, belop, belopkode, dato_periode_fra, dato_periode_til,
                       dato_postert, aar, person_id, posteringtypekode, transaksjonskode,
                       dato_grunnlag, vedtak_id, artkode, kapittel, post, underpost,
                       bruker_id_saksbehandler, aetatenhet_ansvarlig, meldekort_id,
                       tabellnavnalias_kilde, objekt_id_kilde)
VALUES (9850, 7700, 'AAP', DATE '2023-01-30', DATE '2023-02-05', DATE '2023-02-17',
        2023, 122, 'ORD', 'AA00', DATE '2023-01-30', 92030, 'ART', '2900', '01', '001',
        'TEST', '4402', 7504, 'MKORT', 7504),
       (9851, 9000, 'AAP', DATE '2023-02-06', DATE '2023-02-12', DATE '2023-02-17',
        2023, 122, 'ORD', 'AA00', DATE '2023-02-06', 92030, 'ART', '2900', '01', '001',
        'TEST', '4402', 7504, 'MKORT', 7504),
       (9852, -700, 'AAP', DATE '2023-01-30', DATE '2023-02-05', DATE '2023-02-17',
        2023, 122, 'ORD', 'AA00', DATE '2023-01-30', 92030, 'ART', '2900', '01', '001',
        'TEST', '4402', 7504, 'MKORT', 7504),
       (9853, 1000, 'AAP', DATE '2023-01-30', DATE '2023-02-05', DATE '2023-02-17',
        2023, 122, 'ORD', 'AA00', DATE '2023-01-30', 92031, 'ART', '2900', '01', '001',
        'TEST', '4402', 7504, 'MKORT', 7504),
       (9854, 50000, 'DAGP', DATE '2023-01-30', DATE '2023-02-05', DATE '2023-02-17',
        2023, 122, 'ORD', 'DP00', DATE '2023-01-30', 92033, 'ART', '2900', '01', '001',
        'TEST', '4402', 7504, 'MKORT', 7504);

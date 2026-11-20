INSERT INTO person (person_id, fodselsnr, etternavn, fornavn)
VALUES (120, '12000000000', 'Summering', 'Test'),
       (121, '12100000000', 'Negativ', 'Test');

INSERT INTO sak (sak_id, sakskode, reg_dato, reg_user, mod_dato, mod_user, tabellnavnalias, objekt_id,
                 aar, lopenrsak, sakstatuskode, aetatenhet_ansvarlig, er_utland)
VALUES (9201, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 120,
        2023, 9201, 'INAKT', '4402', 'N'),
       (9202, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 121,
        2023, 9202, 'INAKT', '4402', 'N');

INSERT INTO vedtak (vedtak_id, sak_id, vedtakstatuskode, vedtaktypekode, utfallkode, rettighetkode,
                    person_id, fra_dato, til_dato, aetatenhet_behandler, lopenrsak, aar, lopenrvedtak,
                    aktfasekode, dato_mottatt)
VALUES (92010, 9201, 'IVERK', 'O', 'JA', 'AAP', 120,
        DATE '2023-01-10', DATE '2023-01-20', '4402', 9201, 2023, 1, 'IKKE', DATE '2023-01-01'),
       (92020, 9202, 'IVERK', 'O', 'JA', 'AAP', 121,
        DATE '2023-01-01', NULL, '4402', 9202, 2023, 1, 'IKKE', DATE '2023-01-01');

INSERT INTO meldekortperiode (aar, periodekode, ukenr_uke1, ukenr_uke2, dato_fra, dato_til)
VALUES (2023, '01', 1, 2, DATE '2023-01-02', DATE '2023-01-15'),
       (2023, '03', 3, 4, DATE '2023-01-16', DATE '2023-01-29'),
       (2023, '19', 19, 20, DATE '2023-05-08', DATE '2023-05-21');

INSERT INTO meldekort (meldekort_id, person_id, aar, periodekode, mkskortkode, beregningstatuskode)
VALUES (7501, 120, 2023, '01', 'E1', 'FERDI'),
       (7502, 120, 2023, '03', 'E1', 'FERDI'),
       (7503, 121, 2023, '19', 'E1', 'FERDI');

INSERT INTO meldekortdag (meldekort_id, ukenr, dagnr, status_arbeidsdag, status_kurs, status_syk, timer_arbeidet)
VALUES (7501, 1, 1, 'J', 'N', 'N', 3),
       (7501, 1, 2, 'J', 'N', 'N', 2),
       (7501, 2, 2, 'J', 'N', 'N', 1),
       (7501, 2, 3, 'J', 'N', 'N', 2),
       (7501, 2, 4, 'J', 'N', 'N', 4),
       (7501, 2, 5, 'N', 'N', 'N', 0),
       (7502, 3, 1, 'J', 'N', 'N', 8),
       (7502, 3, 2, 'J', 'N', 'N', 8),
       (7502, 3, 3, 'J', 'N', 'N', 8),
       (7502, 3, 4, 'J', 'N', 'N', 8),
       (7502, 3, 5, 'J', 'N', 'N', 8),
       (7502, 4, 1, 'J', 'N', 'N', 8),
       (7502, 4, 2, 'J', 'N', 'N', 8),
       (7502, 4, 3, 'J', 'N', 'N', 8),
       (7502, 4, 4, 'J', 'N', 'N', 8),
       (7502, 4, 5, 'J', 'N', 'N', 8),
       (7503, 19, 1, 'J', 'N', 'N', 3);

INSERT INTO postering (postering_id, belop, belopkode, dato_periode_fra, dato_periode_til,
                       dato_postert, aar, person_id, posteringtypekode, transaksjonskode,
                       dato_grunnlag, vedtak_id, artkode, kapittel, post, underpost,
                       bruker_id_saksbehandler, aetatenhet_ansvarlig, meldekort_id,
                       tabellnavnalias_kilde, objekt_id_kilde)
VALUES (9800, 7700, 'AAP', DATE '2023-01-10', DATE '2023-01-11', DATE '2023-01-20',
        2023, 120, 'ORD', 'AA00', DATE '2023-01-10', 92010, 'ART', '2900', '01', '001',
        'TEST', '4402', 7501, 'MKORT', 7501),
       (9801, 7700, 'AAP', DATE '2023-01-12', DATE '2023-01-15', DATE '2023-01-20',
        2023, 120, 'ORD', 'AA00', DATE '2023-01-10', 92010, 'ART', '2900', '01', '001',
        'TEST', '4402', 7501, 'MKORT', 7501),
       (9802, -1000, 'AAP', DATE '2023-01-12', DATE '2023-01-15', DATE '2023-01-20',
        2023, 120, 'ORD', 'AA00', DATE '2023-01-10', 92010, 'ART', '2900', '01', '001',
        'TEST', '4402', 7501, 'MKORT', 7501),
       (9803, -6600, 'AAP', DATE '2023-01-16', DATE '2023-01-20', DATE '2023-02-03',
        2023, 120, 'ORD', 'AA00', DATE '2023-01-16', 92010, 'ART', '2900', '01', '001',
        'TEST', '4402', 7502, 'MKORT', 7502),
       (9804, -6000, 'AAP', DATE '2023-05-08', DATE '2023-05-21', DATE '2023-05-25',
        2023, 121, 'ORD', 'AA00', DATE '2023-05-08', 92020, 'ART', '2900', '01', '001',
        'TEST', '4402', 7503, 'MKORT', 7503),
       (9805, 6600, 'AAP', DATE '2023-01-16', DATE '2023-01-20', DATE '2023-02-03',
        2023, 120, 'ORD', 'AA00', DATE '2023-01-16', 92010, 'ART', '2900', '01', '001',
        'TEST', '4402', 7502, 'MKORT', 7502),
       (9806, 5000, 'AAP', DATE '2023-05-08', DATE '2023-05-21', DATE '2023-05-25',
        2023, 121, 'ORD', 'AA00', DATE '2023-05-08', 92020, 'ART', '2900', '01', '001',
        'TEST', '4402', 7503, 'MKORT', 7503);

-- Syntetiske data inspirert av CSV-uttrekkets perioder rundt 1. mai og avkortede meldekortperioder.
-- Personer, identifikatorer, beløp, satser og timer er konstruert for testene.
INSERT INTO person (person_id, fodselsnr, etternavn, fornavn)
VALUES (123, '12300000000', 'Maiskifte', 'Test'),
       (124, '12400000000', 'Delperiode', 'Test');

INSERT INTO sak (sak_id, sakskode, reg_dato, reg_user, mod_dato, mod_user, tabellnavnalias, objekt_id,
                 aar, lopenrsak, sakstatuskode, aetatenhet_ansvarlig, er_utland)
VALUES (9204, 'AA', DATE '2024-01-01', 'TEST', DATE '2024-01-01', 'TEST', 'PERS', 123,
        2024, 9204, 'INAKT', '4402', 'N'),
       (9205, 'AA', DATE '2024-01-01', 'TEST', DATE '2024-01-01', 'TEST', 'PERS', 124,
        2024, 9205, 'INAKT', '4402', 'N');

INSERT INTO vedtak (vedtak_id, sak_id, vedtakstatuskode, vedtaktypekode, utfallkode, rettighetkode,
                    person_id, fra_dato, til_dato, aetatenhet_behandler, lopenrsak, aar, lopenrvedtak,
                    aktfasekode, dato_mottatt)
VALUES (92040, 9204, 'AVSLU', 'O', 'JA', 'AAP', 123,
        DATE '2024-05-01', DATE '2025-04-30', '4402', 9204, 2024, 1, 'IKKE', DATE '2024-05-01'),
       (92041, 9204, 'AVSLU', 'E', 'JA', 'AAP', 123,
        DATE '2025-05-01', DATE '2026-04-30', '4402', 9204, 2024, 2, 'IKKE', DATE '2025-05-01'),
       (92042, 9204, 'IVERK', 'E', 'JA', 'AAP', 123,
        DATE '2026-05-01', NULL, '4402', 9204, 2024, 3, 'IKKE', DATE '2026-05-01'),
       (92050, 9205, 'AVSLU', 'O', 'JA', 'AAP', 124,
        DATE '2024-09-18', DATE '2024-10-10', '4402', 9205, 2024, 1, 'IKKE', DATE '2024-09-18');

INSERT INTO vedtakfakta (vedtak_id, vedtakfaktakode, vedtakverdi, reg_dato)
VALUES (92040, 'DAGSMBT', '600', DATE '2024-05-01'),
       (92040, 'JUSTERTG', 'NyG2024', DATE '2024-05-01'),
       (92041, 'DAGSMBT', '700', DATE '2025-05-01'),
       (92041, 'JUSTERTG', 'NyG2025', DATE '2025-05-01'),
       (92042, 'DAGSMBT', '800', DATE '2026-05-01'),
       (92042, 'JUSTERTG', 'NyG2026', DATE '2026-05-01');

INSERT INTO meldekortperiode (aar, periodekode, ukenr_uke1, ukenr_uke2, dato_fra, dato_til)
VALUES (2025, '18', 18, 19, DATE '2025-04-28', DATE '2025-05-11'),
       (2024, '38', 38, 39, DATE '2024-09-16', DATE '2024-09-29'),
       (2024, '40', 40, 41, DATE '2024-09-30', DATE '2024-10-13');

INSERT INTO meldekort (meldekort_id, person_id, aar, periodekode, mkskortkode, beregningstatuskode)
VALUES (7505, 123, 2025, '18', 'E1', 'FERDI'),
       (7506, 124, 2024, '38', 'E1', 'FERDI'),
       (7507, 124, 2024, '40', 'E1', 'FERDI');

INSERT INTO meldekortdag (meldekort_id, ukenr, dagnr, status_arbeidsdag, status_kurs, status_syk, timer_arbeidet)
VALUES (7505, 18, 1, 'N', 'N', 'N', 0),
       (7505, 18, 2, 'N', 'N', 'N', 0),
       (7505, 18, 3, 'N', 'N', 'N', 0),
       (7505, 18, 4, 'N', 'N', 'N', 0),
       (7505, 19, 7, 'J', 'N', 'N', 2.5),
       (7506, 38, 1, 'J', 'N', 'N', 8),
       (7506, 38, 2, 'J', 'N', 'N', 8),
       (7506, 38, 3, 'J', 'N', 'N', 2.5),
       (7506, 39, 3, 'J', 'N', 'N', 1.5),
       (7506, 39, 7, 'J', 'N', 'N', 3),
       (7507, 40, 1, 'J', 'N', 'N', 2.5),
       (7507, 41, 4, 'J', 'N', 'N', 1.5),
       (7507, 41, 5, 'J', 'N', 'N', 8),
       (7507, 41, 7, 'J', 'N', 'N', 8);

INSERT INTO postering (postering_id, belop, belopkode, dato_periode_fra, dato_periode_til,
                       dato_postert, aar, person_id, posteringtypekode, transaksjonskode,
                       dato_grunnlag, vedtak_id, artkode, kapittel, post, underpost,
                       bruker_id_saksbehandler, aetatenhet_ansvarlig, meldekort_id,
                       tabellnavnalias_kilde, objekt_id_kilde)
VALUES (9860, 1800, 'AAP', DATE '2025-04-28', DATE '2025-04-30', DATE '2025-05-15',
        2025, 123, 'ORD', 'AA00', DATE '2025-04-28', 92040, 'ART', '2900', '01', '001',
        'TEST', '4402', 7505, 'MKORT', 7505),
       (9861, 4900, 'AAP', DATE '2025-05-01', DATE '2025-05-11', DATE '2025-05-15',
        2025, 123, 'ORD', 'AA00', DATE '2025-05-01', 92041, 'ART', '2900', '01', '001',
        'TEST', '4402', 7505, 'MKORT', 7505),
       (9862, 4000, 'AAP', DATE '2024-09-18', DATE '2024-09-29', DATE '2024-10-04',
        2024, 124, 'ORD', 'AA00', DATE '2024-09-18', 92050, 'ART', '2900', '01', '001',
        'TEST', '4402', 7506, 'MKORT', 7506),
       (9863, 3000, 'AAP', DATE '2024-09-30', DATE '2024-10-10', DATE '2024-10-18',
        2024, 124, 'ORD', 'AA00', DATE '2024-09-30', 92050, 'ART', '2900', '01', '001',
        'TEST', '4402', 7507, 'MKORT', 7507);

-- MELDEKORTPERIODE er en global kalender med overlappende toukersperioder (uke 33-34, 34-35, 35-36),
-- slik at hver dato dekkes av to perioder. Hvilken periode som gjelder for en bruker avgjøres av
-- brukerens meldekortsyklus, som vi kan utlede fra brukerens meldekort.

insert into MELDEKORTPERIODE (AAR, PERIODEKODE, UKENR_UKE1, UKENR_UKE2, DATO_FRA, DATO_TIL)
values (2026, '33', 33, 34, DATE '2026-08-10', DATE '2026-08-23'),
       (2026, '32', 32, 33, DATE '2026-08-03', DATE '2026-08-16'),
       (2026, '31', 31, 32, DATE '2026-07-27', DATE '2026-08-09'),
       (2026, '34', 34, 35, DATE '2026-08-17', DATE '2026-08-30'),
       (2026, '35', 35, 36, DATE '2026-08-24', DATE '2026-09-06');

-- Bruker som starter meldeperiodene i oddetallsuker (uke 33-34)
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (400, '40000000000', 'Oddetall', 'Testesen');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEKORTKODE)
values (40001, 400, 2026, '33', 'E1', 'KLAR', 'AT'),
       (40002, 400, 2026, '31', 'E1', 'FERDI', 'AT');

-- Bruker som starter meldeperiodene i partallsuker (uke 34-35)
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (401, '40100000000', 'Partall', 'Testesen');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEKORTKODE)
values (40101, 401, 2026, '34', 'E1', 'KLAR', 'AT'),
       (40102, 401, 2026, '32', 'E1', 'FERDI', 'AT');

-- Bruker uten meldekort — har ingen kjent meldekortsyklus
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (402, '40200000000', 'Utenmeldekort', 'Testesen');

-- Testdata for startuke på siste meldekort (meldegruppe FY/ATTF, beregningsstatus KLAR/FERDI)

-- Person med flere AAP-meldekort, der det siste godkjente (KLAR) er i periode 51 i 2026
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (500, '50000000001', 'Meldesen', 'Kort');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50001, 500, 2026, '49', 'E1', 'FERDI', 'ATTF'),
       (50002, 500, 2026, '51', 'E1', 'KLAR', 'ATTF');

-- Nyere meldekort med annen beregningsstatus skal ikke påvirke resultatet
insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50003, 500, 2026, '53', 'E1', 'OPPRE', 'ATTF');

-- Nyere meldekort i annen meldegruppe skal ikke påvirke resultatet
insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50004, 500, 2027, '01', 'E1', 'FERDI', 'DAGP');

-- Person med kun meldekort i annen meldegruppe enn FY/ATTF
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (501, '50000000002', 'Dagpengesen', 'Uten');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50011, 501, 2026, '10', 'E1', 'FERDI', 'DAGP');

-- Person uten meldekort
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (502, '50000000003', 'Kortløs', 'Inga');

-- Person med periodekode med ledende null, for å verifisere tolkningen som tall
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (503, '50000000004', 'Nullesen', 'Ledende');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50021, 503, 2025, '05', 'E1', 'FERDI', 'ATTF');

-- Person der siste meldekort er i meldegruppe FY
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (504, '50000000005', 'Fysesen', 'Frida');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50031, 504, 2026, '10', 'E1', 'FERDI', 'ATTF'),
       (50032, 504, 2026, '12', 'E1', 'FERDI', 'FY');

-- Person med kun meldekort som ikke har beregningsstatus KLAR eller FERDI
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (505, '50000000006', 'Uferdigsen', 'Ola');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50041, 505, 2026, '20', 'E1', 'OPPRE', 'ATTF');

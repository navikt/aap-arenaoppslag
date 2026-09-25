-- Testdata for startuke på siste meldekort (meldegruppe ATTF)

-- Person med flere AAP-meldekort, der det siste er i periode 53 i 2026
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (500, '50000000001', 'Meldesen', 'Kort');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50001, 500, 2026, '49', 'E1', 'FERDI', 'ATTF'),
       (50002, 500, 2026, '51', 'E1', 'FERDI', 'ATTF'),
       (50003, 500, 2026, '53', 'E1', 'OPPRE', 'ATTF');

-- Nyere meldekort i annen meldegruppe skal ikke påvirke resultatet
insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50004, 500, 2027, '01', 'E1', 'OPPRE', 'DAGP');

-- Person med kun meldekort i annen meldegruppe enn ATTF
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (501, '50000000002', 'Dagpengesen', 'Uten');

insert into MELDEKORT (MELDEKORT_ID, PERSON_ID, AAR, PERIODEKODE, MKSKORTKODE, BEREGNINGSTATUSKODE, MELDEGRUPPEKODE)
values (50011, 501, 2026, '10', 'E1', 'FERDI', 'DAGP');

-- Person uten meldekort
insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (502, '50000000003', 'Kortløs', 'Inga');


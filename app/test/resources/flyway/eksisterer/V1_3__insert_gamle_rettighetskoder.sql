-- person med kun vedtak med gamle rettighetskoder
insert into PERSON(PERSON_ID, FODSELSNR) values(992, 'kun_gamle');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('994321', 'IVERK', 'O', 'JA', 'ABOUT', '992', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2015', 'DD.MM.RRRR')),
    ('9912345', 'IVERK', 'O', 'JA', 'ATIF', '992', to_date('31.12.2016', 'DD.MM.RRRR'),to_date('01.01.2021', 'DD.MM.RRRR'));

-- person med vedtak med forskjellige vedtak rettighetkoder
insert into PERSON(PERSON_ID, FODSELSNR) values(996, 'kun_nye');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('995555', 'IVERK', 'O', 'JA', 'AAP', '996', to_date('27.08.2020', 'DD.MM.RRRR'),NULL),
    ('995556', 'AVSLU', 'O', 'JA', 'AAP', '996', to_date('27.08.2019', 'DD.MM.RRRR'),to_date('01.01.2022', 'DD.MM.RRRR'));


-- person med vedtak med forskjellige vedtakstype-kode
insert into PERSON(PERSON_ID, FODSELSNR) values(997, 'blanding');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('994444', 'IVERK', 'O', 'JA', 'AFLYT', '997', to_date('27.08.2019', 'DD.MM.RRRR'),to_date('04.02.2020', 'DD.MM.RRRR')),
    ('994445', 'IVERK', 'E', 'JA', 'AHJMR', '997', to_date('27.08.2016', 'DD.MM.RRRR'),to_date('04.02.2019', 'DD.MM.RRRR')),
    ('994446', 'IVERK', 'G', 'JA', 'AAP', '997', to_date('31.12.2016', 'DD.MM.RRRR'),NULL);

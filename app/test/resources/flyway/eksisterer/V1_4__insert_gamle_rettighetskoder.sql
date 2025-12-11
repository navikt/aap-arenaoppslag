-- person med kun vedtak med gamle rettighetskoder
insert into PERSON(PERSON_ID, FODSELSNR) values(992, 'kun_gamle');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('994321', 1,'IVERK', 'O', 'JA', 'ABOUT', '992', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2015', 'DD.MM.RRRR')),
    ('9912345', 2,'IVERK', 'O', 'JA', 'ATIF', '992', to_date('31.12.2016', 'DD.MM.RRRR'),to_date('01.01.2021', 'DD.MM.RRRR'));

-- person med vedtak med forskjellige vedtak rettighetkoder
insert into PERSON(PERSON_ID, FODSELSNR) values(996, 'kun_nye');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('995555', 3, 'IVERK', 'O', 'JA', 'AAP', '996', to_date('27.08.2019', 'DD.MM.RRRR'),NULL),
    ('995556', 4, 'AVSLU', 'O', 'JA', 'AA115', '996', to_date('27.08.2019', 'DD.MM.RRRR'),to_date('01.01.2025', 'DD.MM.RRRR')),
    ('995557', 5, 'IVERK', 'S', 'JA', 'AA115', '996', to_date('27.08.2024', 'DD.MM.RRRR'),NULL);


-- person med vedtak med forskjellige vedtakstype-kode
insert into PERSON(PERSON_ID, FODSELSNR) values(997, 'blanding');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('994444', 6, 'AVSLU', 'O', 'JA', 'AFLYT', '997', to_date('27.08.2023', 'DD.MM.RRRR'),to_date('04.02.2024', 'DD.MM.RRRR')),
    ('994445', 7, 'IVERK', 'E', 'JA', 'AHJMR', '997', to_date('27.08.2022', 'DD.MM.RRRR'),to_date('04.02.2024', 'DD.MM.RRRR')),
    ('994446', 8, 'IVERK', 'G', 'JA', 'AAP', '997', to_date('31.12.2022', 'DD.MM.RRRR'),NULL),
    -- disse tre skal filtreres ut
    ('994447', 9, 'IVERK', 'G', 'JA', 'AAP', '997', to_date('31.12.2016', 'DD.MM.RRRR'),NULL),
    ('994448', 10, 'IVERK', 'G', 'JA', 'AAP', '997', NULL,NULL),
    ('994449', 11, 'IVERK', 'G', 'JA', 'AAP', '997', to_date('03.01.2020', 'DD.MM.RRRR'),to_date('31.12.2017', 'DD.MM.RRRR'));


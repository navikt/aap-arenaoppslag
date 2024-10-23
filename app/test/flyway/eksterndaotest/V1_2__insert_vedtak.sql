-- hente ut gyldig minimumstruktur for enkelt vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(1, '123');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values('1234', 'IVERK', 'O', 'JA', 'AAP', '1', to_date('30.08.2022', 'DD.MM.RRRR'),to_date('30.08.2023', 'DD.MM.RRRR'));


-- hente ut gyldige minimumsstrukturer for flere vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(2, '321');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('4321', 'IVERK', 'O', 'JA', 'AAP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2018', 'DD.MM.RRRR')),
    ('12345', 'IVERK', 'O', 'JA', 'AAP', '2', to_date('31.12.2019', 'DD.MM.RRRR'),to_date('01.01.2023', 'DD.MM.RRRR'));


-- person uten vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(3, 'ingenvedtak');


-- person med vedtak som ikke er valid
insert into PERSON(PERSON_ID, FODSELSNR) values(4, 'invalid');

insert into VEDTAK (VEDTAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('1333', '0', 'O', 'JA', 'AAP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1334', 'IVERK', 'X', 'JA', 'AAP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1335', 'IVERK', 'O', 'NO', 'AAP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1336', 'IVERK', 'O', 'JA', 'OOP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR'));
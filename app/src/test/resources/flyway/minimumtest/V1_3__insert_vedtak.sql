-- hente ut gyldig minimumstruktur for enkelt vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(1, '123');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values('1234', 1,'IVERK', 'O', 'JA', 'AAP', '1', to_date('30.08.2022', 'DD.MM.RRRR'),to_date('30.08.2023', 'DD.MM.RRRR'));


-- hente ut gyldige minimumsstrukturer for flere vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(2, '321');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('4321', 2,'IVERK', 'O', 'JA', 'AAP', '2', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2018', 'DD.MM.RRRR')),
    ('12345', 3,'IVERK', 'O', 'JA', 'AAP', '2', to_date('31.12.2019', 'DD.MM.RRRR'),to_date('01.01.2023', 'DD.MM.RRRR'));


-- person uten vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(3, 'ingenvedtak');


-- person med vedtak som ikke er valid
insert into PERSON(PERSON_ID, FODSELSNR) values(4, 'invalid');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('1333', 4,'0', 'O', 'JA', 'AAP', '4', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1334', 5, 'IVERK', 'X', 'JA', 'AAP', '4', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1335', 6, 'IVERK', 'O', 'NO', 'AAP', '4', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('1336', 7,'IVERK', 'O', 'JA', 'OOP', '4', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR'));


-- person med blanding av invalid og valid vedtak
insert into PERSON(PERSON_ID, FODSELSNR) values(5, 'somevalid');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('27', 8, '0', 'O', 'JA', 'AAP', '5', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('28', 9, 'IVERK', 'X', 'JA', 'AAP', '5', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('29', 10, 'IVERK', 'O', 'NO', 'AAP', '5', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('30', 11, 'IVERK', 'O', 'JA', 'OOP', '5', to_date('27.08.2010', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('31', 12, 'IVERK', 'O', 'JA', 'AAP', '5', to_date('30.08.2022', 'DD.MM.RRRR'), to_date('04.02.2023', 'DD.MM.RRRR'));


-- person med vedtak med forskjellige vedtak statuskoder
insert into PERSON(PERSON_ID, FODSELSNR) values(6, 'statuskode');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('5555', 13, 'IVERK', 'O', 'JA', 'AAP', '6', to_date('27.08.2022', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('5556', 14, 'AVSLU', 'O', 'JA', 'AAP', '6', to_date('27.08.2019', 'DD.MM.RRRR'),to_date('01.01.2023', 'DD.MM.RRRR'));


-- person med vedtak med forskjellige vedtakstype-kode
insert into PERSON(PERSON_ID, FODSELSNR) values(7, 'typekode');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values
    ('4444', 15, 'IVERK', 'O', 'JA', 'AAP', '7', to_date('27.08.2022', 'DD.MM.RRRR'),to_date('04.02.2023', 'DD.MM.RRRR')),
    ('4445', 16, 'IVERK', 'E', 'JA', 'AAP', '7', to_date('27.08.2019', 'DD.MM.RRRR'),to_date('04.02.2022', 'DD.MM.RRRR')),
    ('4446', 17, 'IVERK', 'G', 'JA', 'AAP', '7', to_date('31.12.2019', 'DD.MM.RRRR'),to_date('01.01.2023', 'DD.MM.RRRR'));


--- vedtak med null til-dato
insert into PERSON(PERSON_ID, FODSELSNR) values(8, 'nulltildato');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE, PERSON_ID, FRA_DATO, TIL_DATO)
values('5', 0, 'IVERK', 'O', 'JA', 'AAP', '8', to_date('30.08.2022', 'DD.MM.RRRR'), NULL);

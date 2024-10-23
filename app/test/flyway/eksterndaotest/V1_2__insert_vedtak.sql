-- hente ut gyldig minimumstruktur for enkelt vedtak
INSERT INTO PERSON(PERSON_ID, FODSELSNR) VALUES(1, '123');

Insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER,
                    UTFALLKODE, BEGRUNNELSE, BRUKERID_ANSVARLIG, AETATENHET_BEHANDLER, AAR, LOPENRSAK, LOPENRVEDTAK,
                    RETTIGHETKODE, AKTFASEKODE, BREV_ID, TOTALBELOP, DATO_MOTTATT, VEDTAK_ID_RELATERT,
                    AVSNITTLISTEKODE_VALGT, PERSON_ID, BRUKERID_BESLUTTER, STATUS_SENSITIV, VEDLEGG_BETPLAN, PARTISJON,
                    OPPSUMMERING_SB2, DATO_UTFORT_DEL1, DATO_UTFORT_DEL2, OVERFORT_NAVI, FRA_DATO, TIL_DATO,
                    SF_OPPFOLGING_ID, STATUS_SOSIALDATA, KONTOR_SOSIALDATA, TEKSTVARIANTKODE, VALGT_BESLUTTER,
                    TEKNISK_VEDTAK, DATO_INNSTILT, ER_UTLAND)
values ('1234', '13489616', 'IVERK', 'O', to_date('30.08.2022', 'DD.MM.RRRR'), 'KSB0502',
        to_date('30.08.2022', 'DD.MM.RRRR'), 'MNA0502', 'JA', 'Syntetisert rettighet', 'KSB0502', '0502', '2022',
        '73981', '5', 'AAP', 'UGJEN', '31028783', null, to_date('30.08.2022', 'DD.MM.RRRR'), '36905534', null,
        '1', 'MNA0502', null, 'N', null, null, null, null, null, to_date('30.08.2022', 'DD.MM.RRRR'), to_date('30.08.2023', 'DD.MM.RRRR'), null,
        'N', null, null, 'MNA0502', null, to_date('17.09.2022', 'DD.MM.RRRR'), 'N');


-- hente ut gyldige minimumsstrukturer
INSERT INTO PERSON(PERSON_ID, FODSELSNR) VALUES(2, '321');

Insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER,
                    UTFALLKODE, BEGRUNNELSE, BRUKERID_ANSVARLIG, AETATENHET_BEHANDLER, AAR, LOPENRSAK, LOPENRVEDTAK,
                    RETTIGHETKODE, AKTFASEKODE, BREV_ID, TOTALBELOP, DATO_MOTTATT, VEDTAK_ID_RELATERT,
                    AVSNITTLISTEKODE_VALGT, PERSON_ID, BRUKERID_BESLUTTER, STATUS_SENSITIV, VEDLEGG_BETPLAN, PARTISJON,
                    OPPSUMMERING_SB2, DATO_UTFORT_DEL1, DATO_UTFORT_DEL2, OVERFORT_NAVI, FRA_DATO, TIL_DATO,
                    SF_OPPFOLGING_ID, STATUS_SOSIALDATA, KONTOR_SOSIALDATA, TEKSTVARIANTKODE, VALGT_BESLUTTER,
                    TEKNISK_VEDTAK, DATO_INNSTILT, ER_UTLAND)
values
    ('4321', '13489616', 'IVERK', 'O', to_date('30.08.2022', 'DD.MM.RRRR'), 'KSB0502',
        to_date('30.08.2022', 'DD.MM.RRRR'), 'MNA0502', 'JA', 'Syntetisert rettighet', 'KSB0502', '0502', '2022',
        '73981', '5', 'AAP', 'UGJEN', '31028783', null, to_date('30.08.2022', 'DD.MM.RRRR'), '36905534', null,
        '2', 'MNA0502', null, 'N', null, null, null, null, null, to_date('27.08.2010', 'DD.MM.RRRR'), to_date('04.02.2018', 'DD.MM.RRRR'), null,
        'N', null, null, 'MNA0502', null, to_date('17.09.2022', 'DD.MM.RRRR'), 'N'),
    ('12345', '13489616', 'IVERK', 'O', to_date('30.08.2022', 'DD.MM.RRRR'), 'KSB0502',
     to_date('30.08.2022', 'DD.MM.RRRR'), 'MNA0502', 'JA', 'Syntetisert rettighet', 'KSB0502', '0502', '2022',
     '73981', '5', 'AAP', 'UGJEN', '31028783', null, to_date('30.08.2022', 'DD.MM.RRRR'), '36905534', null,
     '2', 'MNA0502', null, 'N', null, null, null, null, null, to_date('31.12.2019', 'DD.MM.RRRR'), to_date('01.01.2023', 'DD.MM.RRRR'), null,
     'N', null, null, 'MNA0502', null, to_date('17.09.2022', 'DD.MM.RRRR'), 'N');
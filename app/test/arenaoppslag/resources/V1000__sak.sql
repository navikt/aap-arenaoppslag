CREATE TABLE PERSON
(
    PERSON_ID NUMBER NOT NULL,
    FODSELSNR VARCHAR2(11) NOT NULL,
    PRIMARY KEY (PERSON_ID)
);

INSERT INTO PERSON(PERSON_ID, FODSELSNR) VALUES(4873545, '1');




CREATE TABLE SAK
(
    SAK_ID               NUMBER       NOT NULL,
    SAKSKODE             VARCHAR2(10) NOT NULL,
    REG_DATO             DATE,
    REG_USER             VARCHAR2(8),
    MOD_DATO             DATE,
    MOD_USER             VARCHAR2(8),
    TABELLNAVNALIAS      VARCHAR2(10) NOT NULL,
    OBJEKT_ID            NUMBER,
    AAR                  NUMBER(4)    NOT NULL,
    LOPENRSAK            NUMBER(7)    NOT NULL,
    DATO_AVSLUTTET       DATE,
    SAKSTATUSKODE        VARCHAR2(5)  NOT NULL,
    ARKIVNOKKEL          VARCHAR2(7),
    AETATENHET_ARKIV     VARCHAR2(8),
    ARKIVHENVISNING      VARCHAR2(255),
    BRUKERID_ANSVARLIG   VARCHAR2(8),
    AETATENHET_ANSVARLIG VARCHAR2(8),
    OBJEKT_KODE          VARCHAR2(10),
    STATUS_ENDRET        DATE,
    PARTISJON            NUMBER(8),
    ER_UTLAND            VARCHAR2(1)  NOT NULL,
    PRIMARY KEY (SAK_ID)
);

CREATE TABLE VEDTAK
(
    VEDTAK_ID              NUMBER       NOT NULL,
    SAK_ID                 NUMBER       NOT NULL,
    VEDTAKSTATUSKODE       VARCHAR2(5)  NOT NULL,
    VEDTAKTYPEKODE         VARCHAR2(10) NOT NULL,
    REG_DATO               DATE,
    REG_USER               VARCHAR2(8),
    MOD_DATO               DATE,
    MOD_USER               VARCHAR2(8),
    UTFALLKODE             VARCHAR2(10),
    BEGRUNNELSE            VARCHAR2(4000),
    BRUKERID_ANSVARLIG     VARCHAR2(8),
    AETATENHET_BEHANDLER   VARCHAR2(8)  NOT NULL,
    AAR                    NUMBER(4)    NOT NULL,
    LOPENRSAK              NUMBER(7)    NOT NULL,
    LOPENRVEDTAK           NUMBER(3)    NOT NULL,
    RETTIGHETKODE          VARCHAR2(10) NOT NULL,
    AKTFASEKODE            VARCHAR2(10) NOT NULL,
    BREV_ID                NUMBER,
    TOTALBELOP             NUMBER(8, 2),
    DATO_MOTTATT           DATE         NOT NULL,
    VEDTAK_ID_RELATERT     NUMBER,
    AVSNITTLISTEKODE_VALGT VARCHAR2(20),
    PERSON_ID              NUMBER,
    BRUKERID_BESLUTTER     VARCHAR2(8),
    STATUS_SENSITIV        VARCHAR2(1),
    VEDLEGG_BETPLAN        VARCHAR2(1),
    PARTISJON              NUMBER(8),
    OPPSUMMERING_SB2       VARCHAR2(4000),
    DATO_UTFORT_DEL1       DATE,
    DATO_UTFORT_DEL2       DATE,
    OVERFORT_NAVI          VARCHAR2(1),
    FRA_DATO               DATE,
    TIL_DATO               DATE,
    SF_OPPFOLGING_ID       NUMBER,
    STATUS_SOSIALDATA      VARCHAR2(1)  NOT NULL,
    KONTOR_SOSIALDATA      VARCHAR2(8),
    TEKSTVARIANTKODE       VARCHAR2(20),
    VALGT_BESLUTTER        VARCHAR2(8),
    TEKNISK_VEDTAK         VARCHAR2(1),
    DATO_INNSTILT          DATE,
    ER_UTLAND              VARCHAR2(1)  NOT NULL,
    PRIMARY KEY (VEDTAK_ID)
);


CREATE TABLE VEDTAKFAKTA
(
    VEDTAK_ID       NUMBER       NOT NULL,
    VEDTAKFAKTAKODE VARCHAR2(10) NOT NULL,
    VEDTAKVERDI     VARCHAR2(2000),
    REG_DATO        DATE,
    REG_USER        VARCHAR2(8),
    MOD_DATO        DATE,
    MOD_USER        VARCHAR2(8),
    PERSON_ID       NUMBER,
    PARTISJON       NUMBER(8),
    PRIMARY KEY (VEDTAK_ID, VEDTAKFAKTAKODE)
);


Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR, LOPENRSAK,
                 DATO_AVSLUTTET, SAKSTATUSKODE, ARKIVNOKKEL, AETATENHET_ARKIV, ARKIVHENVISNING, BRUKERID_ANSVARLIG,
                 AETATENHET_ANSVARLIG, OBJEKT_KODE, STATUS_ENDRET, PARTISJON, ER_UTLAND)
values ('13489616', 'INDIV', to_date('02.02.2022', 'DD.MM.RRRR'), 'IMB0826', to_date('22.02.2023', 'DD.MM.RRRR'),
        'MM0826', 'PERS', '4873545', '2021', '285758', null, 'INAKT', null, null, null, 'IMB0826', '0826', null,
        to_date('22.02.2023', 'DD.MM.RRRR'), null, 'N');


Insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER,
                    UTFALLKODE, BEGRUNNELSE, BRUKERID_ANSVARLIG, AETATENHET_BEHANDLER, AAR, LOPENRSAK, LOPENRVEDTAK,
                    RETTIGHETKODE, AKTFASEKODE, BREV_ID, TOTALBELOP, DATO_MOTTATT, VEDTAK_ID_RELATERT,
                    AVSNITTLISTEKODE_VALGT, PERSON_ID, BRUKERID_BESLUTTER, STATUS_SENSITIV, VEDLEGG_BETPLAN, PARTISJON,
                    OPPSUMMERING_SB2, DATO_UTFORT_DEL1, DATO_UTFORT_DEL2, OVERFORT_NAVI, FRA_DATO, TIL_DATO,
                    SF_OPPFOLGING_ID, STATUS_SOSIALDATA, KONTOR_SOSIALDATA, TEKSTVARIANTKODE, VALGT_BESLUTTER,
                    TEKNISK_VEDTAK, DATO_INNSTILT, ER_UTLAND)
values ('36905537', '13489616', 'IVERK', 'S', to_date('30.08.2022', 'DD.MM.RRRR'), 'KSB0502',
        to_date('30.08.2022', 'DD.MM.RRRR'), 'MNA0502', 'JA', 'Syntetisert rettighet', 'KSB0502', '0502', '2022',
        '73981', '5', 'BASI', 'UGJEN', '31028783', null, to_date('30.08.2022', 'DD.MM.RRRR'), '36905534', null,
        '4873545', 'MNA0502', null, 'N', null, null, null, null, null, to_date('30.08.2022', 'DD.MM.RRRR'), null, null,
        'N', null, null, 'MNA0502', null, to_date('17.09.2022', 'DD.MM.RRRR'), 'N');


Insert into VEDTAKFAKTA (VEDTAK_ID, VEDTAKFAKTAKODE, VEDTAKVERDI, REG_DATO, REG_USER, MOD_DATO, MOD_USER, PERSON_ID,
                         PARTISJON)
values ('36905537', 'DAGS', '255', to_date('30.08.2022', 'DD.MM.RRRR'), 'KSB0502', to_date('30.08.2022', 'DD.MM.RRRR'),
        'KSB0502', '4873545', null);
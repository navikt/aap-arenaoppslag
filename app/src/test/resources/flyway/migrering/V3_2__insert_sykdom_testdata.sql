-- Testdata for sykdom-endepunktet i migrering (saksnummer "2023-504").
-- Datoene er relative til CURRENT_DATE, så "gjeldende" 11-5-vedtak alltid dekker dagens dato.
-- Bare vedtak 91045 skal velges: det er det nyeste iverksatte, innvilgede AA115-vedtaket som dekker i dag.

insert into PERSON(PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN)
values (203, '20300000000', 'Sykdomsvurdert', 'Testesen');

Insert into SAK (SAK_ID, SAKSKODE, REG_DATO, REG_USER, MOD_DATO, MOD_USER, TABELLNAVNALIAS, OBJEKT_ID, AAR,
                 LOPENRSAK, DATO_AVSLUTTET, SAKSTATUSKODE, AETATENHET_ANSVARLIG, PARTISJON, ER_UTLAND)
values (9104, 'AA', DATE '2023-01-01', 'TEST', DATE '2023-01-01', 'TEST', 'PERS', 203, 2023, 504, null, 'AKTIV',
        '4402', null, 'N');

insert into VEDTAK (VEDTAK_ID, SAK_ID, VEDTAKSTATUSKODE, VEDTAKTYPEKODE, UTFALLKODE, RETTIGHETKODE,
                    PERSON_ID, FRA_DATO, TIL_DATO, AETATENHET_BEHANDLER, LOPENRSAK, AAR, LOPENRVEDTAK,
                    AKTFASEKODE, DATO_MOTTATT, BEGRUNNELSE)
values
    -- Utløpt 11-5-vedtak: skal ikke velges
    (91041, 9104, 'IVERK', 'O', 'JA', 'AA115', 203,
     DATEADD('DAY', -400, CURRENT_DATE), DATEADD('DAY', -200, CURRENT_DATE), '4402', 504, 2023, 1, 'IKKE',
     DATEADD('DAY', -400, CURRENT_DATE), 'Utløpt vurdering'),
    -- Avsluttet 11-5-vedtak som dekker i dag: skal ikke velges (bare IVERK teller)
    (91042, 9104, 'AVSLU', 'O', 'JA', 'AA115', 203,
     DATEADD('DAY', -10, CURRENT_DATE), null, '4402', 504, 2023, 2, 'IKKE',
     DATEADD('DAY', -10, CURRENT_DATE), 'Avsluttet vurdering'),
    -- Avslag på 11-5 som dekker i dag: skal ikke velges
    (91043, 9104, 'IVERK', 'O', 'NEI', 'AA115', 203,
     DATEADD('DAY', -5, CURRENT_DATE), null, '4402', 504, 2023, 3, 'IKKE',
     DATEADD('DAY', -5, CURRENT_DATE), 'Avslått vurdering'),
    -- Eldre gjeldende 11-5-vedtak: taper mot det nyere
    (91044, 9104, 'IVERK', 'O', 'JA', 'AA115', 203,
     DATEADD('DAY', -100, CURRENT_DATE), null, '4402', 504, 2023, 4, 'IKKE',
     DATEADD('DAY', -100, CURRENT_DATE), 'Eldre vurdering'),
    -- Siste gjeldende 11-5-vedtak: dette skal returneres
    (91045, 9104, 'IVERK', 'E', 'JA', 'AA115', 203,
     DATEADD('DAY', -30, CURRENT_DATE), DATEADD('DAY', 30, CURRENT_DATE), '4402', 504, 2023, 5, 'IKKE',
     DATEADD('DAY', -30, CURRENT_DATE), 'Arbeidsevnen er nedsatt med minst halvparten'),
    -- Fremtidig 11-5-vedtak: skal ikke velges
    (91046, 9104, 'IVERK', 'E', 'JA', 'AA115', 203,
     DATEADD('DAY', 10, CURRENT_DATE), null, '4402', 504, 2023, 6, 'IKKE',
     DATEADD('DAY', 10, CURRENT_DATE), 'Fremtidig vurdering'),
    -- AAP-vedtak som dekker i dag: skal ikke velges fordi rettigheten ikke er 11-5
    (91047, 9104, 'IVERK', 'O', 'JA', 'AAP', 203,
     DATEADD('DAY', -1, CURRENT_DATE), null, '4402', 504, 2023, 7, 'IKKE',
     DATEADD('DAY', -1, CURRENT_DATE), 'AAP-vedtak');

insert into VILKAARVURDERING (VILKAARVURDERING_ID, VEDTAKTYPEKODE, VILKAARKODE, VEDTAK_ID, REG_DATO, REG_USER,
                              MOD_DATO, MOD_USER, RETTIGHETKODE, AKTFASEKODE, VILKAARSTATUSKODE, VURDERT_AV,
                              BEGRUNNELSE)
values (910451, 'E', 'SYKSKADLYT', 91045, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AA115', 'IKKE', 'J',
        'TEST01', 'Dokumentert gjennom legeerklæring'),
       (910452, 'E', 'INNTNEDS', 91045, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AA115', 'IKKE', 'N',
        'TEST01', null),
       (910453, 'E', 'AAARBEVNE', 91045, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AA115', 'IKKE', 'V',
        'TEST01', null),
       (910441, 'O', 'SYKSKADLYT', 91044, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AA115', 'IKKE', 'J',
        'TEST01', null);

-- Diagnoser er knyttet til personen via ATTFORINGOPPLYSNING, ikke til sak eller vedtak.
insert into DIAGNOSEKLASSE (DIAGNOSEKLASSEKODE, DIAGNOSEKLASSENAVN)
values ('ICPC2', 'ICPC-2'),
       ('ICD10', 'ICD-10');

insert into DIAGNOSE (DIAGNOSEKODE, DIAGNOSEKLASSEKODE, DIAGNOSENAVN, DATO_FRA, REG_DATO, REG_USER, MOD_DATO,
                      MOD_USER, HOVEDDIAGNOSE_JN)
values ('L84', 'ICPC2', 'Ryggsyndrom uten smerteutstråling', DATE '2000-01-01', DATE '2000-01-01', 'TEST',
        DATE '2000-01-01', 'TEST', 'J'),
       ('P76', 'ICPC2', 'Depresjon', DATE '2000-01-01', DATE '2000-01-01', 'TEST', DATE '2000-01-01', 'TEST', 'J'),
       ('M54', 'ICD10', 'Ryggsmerter', DATE '2000-01-01', DATE '2000-01-01', 'TEST', DATE '2000-01-01', 'TEST', 'J');

insert into ATTFORINGOPPLYSNING (ATTFORINGOPPLYSNING_ID, PERSON_ID, ATTFORINGAARSAKKODE, SJEKK_NEI_INNSYN)
values (9201, 203, 'HELSE', 'J'),
       -- Annen person, skal aldri lekke inn i sykdomsvurderingen for person 203
       (9202, 200, 'HELSE', 'J');

insert into MEDISINSK_OPPLYSNING (MEDISINSK_OPPLYSNING_ID, ATTFORINGSOPPLYSNING_ID, DIAGNOSEKLASSEKODE,
                                  DIAGNOSETYPEKODE, DIAGNOSEKODE, KILDE_DATO, KILDEKODE)
values (92011, 9201, 'ICPC2', 'HOVED', 'L84', DATE '2023-02-01', 'LEGE'),
       (92012, 9201, 'ICD10', 'BI', 'M54', DATE '2023-03-01', 'LEGE'),
       (92013, 9201, 'ICPC2', 'BI', 'P76', DATE '2023-04-01', 'LEGE'),
       (92021, 9202, 'ICPC2', 'HOVED', 'P76', DATE '2023-02-01', 'LEGE');





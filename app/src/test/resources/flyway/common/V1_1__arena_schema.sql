--------------------------------------------------------------------------------
-- AKTIVITETFASE
--------------------------------------------------------------------------------
CREATE TABLE "AKTIVITETFASE"
(
    "AKTFASEKODE" VARCHAR2(10)         NOT NULL,
    "AKTFASENAVN" VARCHAR2(35)         NOT NULL,
    "DATO_FRA"    DATE DEFAULT CURRENT_DATE NOT NULL,
    "DATO_TIL"    DATE                 NOT NULL,
    "REG_DATO"    DATE,
    "MOD_DATO"    DATE,
    "REG_USER"    VARCHAR2(8),
    "MOD_USER"    VARCHAR2(8),
    CONSTRAINT "AKTFAS_PK" PRIMARY KEY ("AKTFASEKODE")
);

COMMENT ON COLUMN "AKTIVITETFASE"."AKTFASEKODE" IS 'Entydig kode på aktivitetsfasen';
COMMENT ON COLUMN "AKTIVITETFASE"."AKTFASENAVN" IS 'Navnet på aktivitestfasekoden.';
COMMENT ON COLUMN "AKTIVITETFASE"."DATO_FRA" IS 'Fra-dato for gyldighetsperioden for koden';
COMMENT ON COLUMN "AKTIVITETFASE"."DATO_TIL" IS 'Til-dato for gyldighetsperioden for koden';
COMMENT ON TABLE "AKTIVITETFASE" IS 'Beskriver faser i et rettighetsløp';

--------------------------------------------------------------------------------
-- BREV_TEKSTVARIANT
--------------------------------------------------------------------------------
CREATE TABLE "BREV_TEKSTVARIANT"
(
    "TEKSTVARIANTKODE" VARCHAR2(20)  NOT NULL,
    "TEKSTVARIANTNAVN" VARCHAR2(255) NOT NULL,
    "REG_USER"         VARCHAR2(8),
    "REG_DATO"         DATE,
    "MOD_USER"         VARCHAR2(8),
    "MOD_DATO"         DATE,
    CONSTRAINT "BREVVAR_PK" PRIMARY KEY ("TEKSTVARIANTKODE")
);

COMMENT ON COLUMN "BREV_TEKSTVARIANT"."TEKSTVARIANTKODE" IS 'Kode for tekstvariant, benyttes på vedtak.';
COMMENT ON COLUMN "BREV_TEKSTVARIANT"."TEKSTVARIANTNAVN" IS 'Tekstvariant slik den vises i GUI.';
COMMENT ON COLUMN "BREV_TEKSTVARIANT"."REG_USER" IS 'Angir hvilken bruker som opprettet raden';
COMMENT ON COLUMN "BREV_TEKSTVARIANT"."REG_DATO" IS 'Angir tidspunkt for når raden ble opprettet';
COMMENT ON COLUMN "BREV_TEKSTVARIANT"."MOD_USER" IS 'Angir hvilken bruker som sist endret raden';
COMMENT ON COLUMN "BREV_TEKSTVARIANT"."MOD_DATO" IS 'Angir tidspunkt for når raden sist ble endret';
COMMENT ON TABLE "BREV_TEKSTVARIANT" IS 'Tabell for defisjon av vedtaksvarianter for brev og GUI.';

--------------------------------------------------------------------------------
-- PERSON
--------------------------------------------------------------------------------
CREATE TABLE "PERSON"
(
    "PERSON_ID"                     NUMBER                            NOT NULL,
    "FODSELSDATO"                   DATE,
    "STATUS_DNR"                    VARCHAR2(1)  DEFAULT 'N'          NOT NULL,
    "PERSONNR"                      NUMBER(5, 0),
    "FODSELSNR"                     VARCHAR2(11),
    "ETTERNAVN"                     VARCHAR2(30)                      NOT NULL,
    "FORNAVN"                       VARCHAR2(30)                      NOT NULL,
    "DATO_FRA"                      DATE         DEFAULT CURRENT_DATE NOT NULL,
    "STATUS_SAMTYKKE"               VARCHAR2(1)  DEFAULT 'N'          NOT NULL,
    "DATO_SAMTYKKE"                 DATE,
    "VERNEPLIKTKODE"                VARCHAR2(5)  DEFAULT NULL,
    "MAALFORM"                      VARCHAR2(2)  DEFAULT 'NO'         NOT NULL,
    "LANDKODE_STATSBORGER"          VARCHAR2(2),
    "KONTONUMMER"                   VARCHAR2(11),
    "STATUS_BILDISP"                VARCHAR2(1),
    "FORMIDLINGSGRUPPEKODE"         VARCHAR2(5)  DEFAULT 'ISERV'      NOT NULL,
    "VIKARGRUPPEKODE"               VARCHAR2(5)  DEFAULT 'IVIK'       NOT NULL,
    "KVALIFISERINGSGRUPPEKODE"      VARCHAR2(5)  DEFAULT 'IVURD'      NOT NULL,
    "RETTIGHETSGRUPPEKODE"          VARCHAR2(5)  DEFAULT 'IYT'        NOT NULL,
    "REG_DATO"                      DATE,
    "REG_USER"                      VARCHAR2(8),
    "MOD_DATO"                      DATE,
    "MOD_USER"                      VARCHAR2(8),
    "AETATORGENHET"                 VARCHAR2(8),
    "LONNSLIPP_EPOST"               VARCHAR2(1),
    "DATO_OVERFORT_AMELDING"        DATE,
    "DATO_SIST_INAKTIV"             DATE,
    "BEGRUNNELSE_FORMIDLINGSGRUPPE" VARCHAR2(2000),
    "HOVEDMAALKODE"                 VARCHAR2(10),
    "BRUKERID_NAV_KONTAKT"          VARCHAR2(8),
    "FR_KODE"                       VARCHAR2(2),
    "ER_DOED"                       VARCHAR2(1),
    "PERSON_ID_STATUS"              VARCHAR2(20) DEFAULT 'AKTIV'      NOT NULL,
    "SPERRET_KOMMENTAR"             VARCHAR2(500),
    "SPERRET_TIL"                   DATE,
    "SPERRET_DATO"                  DATE,
    "SPERRET_AV"                    VARCHAR2(8),
    CONSTRAINT "PERS_DOED_CK5" CHECK (er_doed in ('J', NULL)),
    CONSTRAINT "PERS_STATJN_CK3" CHECK (STATUS_BILDISP IN ('J', 'N')),
    CONSTRAINT "PERS_STATJN_CK" CHECK (status_dnr in ('J', 'N')),
    CONSTRAINT "PERS_STATJN_CK2" CHECK (status_samtykke in ('J', 'N', 'B', 'G')),
    CONSTRAINT "PERSON_CK6" CHECK (PERSON_ID_STATUS
        IN ('AKTIV', 'DUPLIKAT_TIL_BEH', 'DUPLIKAT', 'ANNULLERES', 'ANNULLERT', 'UGYLDIG', 'SPERRET', 'RESERVERT'))
);
ALTER TABLE "PERSON"
    ADD CONSTRAINT "PERS_PK" PRIMARY KEY ("PERSON_ID");
ALTER TABLE "PERSON"
    ADD CONSTRAINT "PERS_UK2" UNIQUE ("FODSELSNR");

COMMENT ON COLUMN "PERSON"."PERSON_ID" IS 'Generert Oracle-sekvens som entydig identifiserer posten';
COMMENT ON COLUMN "PERSON"."FODSELSDATO" IS 'Fødselsdato';
COMMENT ON COLUMN "PERSON"."STATUS_DNR" IS 'Settes J hvis FODSELSNR er et DNR';
COMMENT ON COLUMN "PERSON"."PERSONNR" IS 'Norsk personnummer eller D-nummer';
COMMENT ON COLUMN "PERSON"."FODSELSNR" IS 'Norsk fødselsnummer';
COMMENT ON COLUMN "PERSON"."ETTERNAVN" IS 'Etternavn';
COMMENT ON COLUMN "PERSON"."FORNAVN" IS 'Fornavn evt med mellomnavn';
COMMENT ON COLUMN "PERSON"."DATO_FRA" IS 'Fra-dato i gyldighetsperiode';
COMMENT ON COLUMN "PERSON"."STATUS_SAMTYKKE" IS 'Status samtykke';
COMMENT ON COLUMN "PERSON"."DATO_SAMTYKKE" IS 'Dato for når siste samtykke er gitt';
COMMENT ON COLUMN "PERSON"."VERNEPLIKTKODE" IS 'Kode for gjennomført verneplikt';
COMMENT ON COLUMN "PERSON"."MAALFORM" IS 'Referanse til EDB_LANGUAGE';
COMMENT ON COLUMN "PERSON"."LANDKODE_STATSBORGER" IS 'Referanse til LAND';
COMMENT ON COLUMN "PERSON"."KONTONUMMER" IS 'Ikke i bruk. Flyttet til kommbruk type: NOKTO Kontonummer for utbetalinger fra Aetat';
COMMENT ON COLUMN "PERSON"."STATUS_BILDISP" IS 'Disponerer bil';
COMMENT ON COLUMN "PERSON"."FORMIDLINGSGRUPPEKODE" IS 'Referanse til FORMIDLINGSGRUPPETYPE personen tilhører';
COMMENT ON COLUMN "PERSON"."VIKARGRUPPEKODE" IS 'Ikke i bruk. Referanse til VIKARGRUPPETYPE';
COMMENT ON COLUMN "PERSON"."KVALIFISERINGSGRUPPEKODE" IS 'Referanse til KVALIFISERINGSGRUPPETYPE. Kalles nå Servicegruppe';
COMMENT ON COLUMN "PERSON"."RETTIGHETSGRUPPEKODE" IS 'Referanse til RETTIGHETSGRUPPETYPE';
COMMENT ON COLUMN "PERSON"."AETATORGENHET" IS 'Referanse til ORGUNITINSTANCE';
COMMENT ON COLUMN "PERSON"."LONNSLIPP_EPOST" IS 'Angir om en person skal motta lønnslipp via epost';
COMMENT ON COLUMN "PERSON"."DATO_OVERFORT_AMELDING" IS 'Dato for første overføring til Amelding';
COMMENT ON COLUMN "PERSON"."DATO_SIST_INAKTIV" IS 'Dato sist inaktiv';
COMMENT ON COLUMN "PERSON"."BEGRUNNELSE_FORMIDLINGSGRUPPE" IS 'Evt saksbehandlers begrunnelse ved setting av formidlingsgruppe';
COMMENT ON COLUMN "PERSON"."HOVEDMAALKODE" IS 'Rereranse til HOVEDMAAL';
COMMENT ON COLUMN "PERSON"."BRUKERID_NAV_KONTAKT" IS 'Referanse til ORGUNITINSTANCE. Kontaktperson hos NAV';
COMMENT ON COLUMN "PERSON"."FR_KODE" IS 'Koding av fortrolige adresser fra folkeregisteret';
COMMENT ON COLUMN "PERSON"."ER_DOED" IS 'Er død';
COMMENT ON COLUMN "PERSON"."PERSON_ID_STATUS" IS 'Status for denne personforekomsten/person_id, ikke generelt for person';
COMMENT ON COLUMN "PERSON"."SPERRET_KOMMENTAR" IS 'Evt. kommentar om sperringen';
COMMENT ON COLUMN "PERSON"."SPERRET_TIL" IS 'Evt. slutt dato for sperringen';
COMMENT ON COLUMN "PERSON"."SPERRET_DATO" IS 'Dato sperren ble satt';
COMMENT ON COLUMN "PERSON"."SPERRET_AV" IS 'Saksbeh. ident for den som etablerte sperren.';
COMMENT ON TABLE "PERSON" IS 'Tabellen omfatter alle personer som NAV har et forhold til';

--------------------------------------------------------------------------------
-- MELDEKORT
--------------------------------------------------------------------------------
CREATE TABLE "MELDEKORT"
(
    "MELDEKORT_ID"                NUMBER       NOT NULL,
    "PERSON_ID"                   NUMBER       NOT NULL,
    "DATO_INNKOMMET"              DATE,
    "DATO_UTSENDT"                DATE,
    "MKSREFERANSE"                VARCHAR2(21),
    "MELDEKORTKODE"               VARCHAR2(5),
    "MKSKORTKODE"                 VARCHAR2(2)  NOT NULL,
    "STATUS_ARBEIDET"             VARCHAR2(1),
    "STATUS_FERIE"                VARCHAR2(1) DEFAULT 'N',
    "STATUS_KURS"                 VARCHAR2(1) DEFAULT NULL,
    "STATUS_NYTT_MELDEKORT"       VARCHAR2(1) DEFAULT 'I',
    "STATUS_SYK"                  VARCHAR2(1) DEFAULT NULL,
    "STATUS_PERIODESPOERSMAAL"    VARCHAR2(1) DEFAULT 'N',
    "STATUS_SOEKER_DAGPENGER"     VARCHAR2(1),
    "STATUS_ANNETFRAVAER_ATTF"    VARCHAR2(1) DEFAULT NULL,
    "STATUS_ATTFORINGSBISTAND"    VARCHAR2(1) DEFAULT 'I',
    "STATUS_ATTFORINGSTILTAK"     VARCHAR2(1) DEFAULT 'I',
    "REG_DATO"                    DATE,
    "REG_USER"                    VARCHAR2(8),
    "MOD_DATO"                    DATE,
    "MOD_USER"                    VARCHAR2(8),
    "AAR"                         NUMBER(4, 0) NOT NULL,
    "PERIODEKODE"                 VARCHAR2(2)  NOT NULL,
    "BEREGNINGSTATUSKODE"         VARCHAR2(5)  NOT NULL,
    "STATUS_ANNETFRAVAER"         VARCHAR2(1),
    "STATUS_FORTSATT_ARBEIDSOKER" VARCHAR2(1),
    "FEIL_PAA_KORT"               VARCHAR2(1),
    "VEILEDNING"                  VARCHAR2(1),
    "KOMMENTAR"                   VARCHAR2(255),
    "MELDEGRUPPEKODE"             VARCHAR2(5),
    "RETURBREVKODE"               VARCHAR2(2),
    "AB_POSTKODE"                 VARCHAR2(1),
    "MELDEKORT_ID_RELATERT"       NUMBER,
    "PARTISJON"                   NUMBER(8, 0),
    CONSTRAINT "MKORT_PK" PRIMARY KEY ("MELDEKORT_ID"),
    CONSTRAINT "MKORT_STATJN_CK1" CHECK (status_arbeidet in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK2" CHECK (status_kurs in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK3" CHECK (status_syk in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK4" CHECK (status_periodespoersmaal in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK5" CHECK (status_annetfravaer in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK6" CHECK (status_fortsatt_arbeidsoker in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK7" CHECK (feil_paa_kort in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_STATJN_CK8" CHECK (veiledning in ('J', 'N', 'I', 'B')),
    CONSTRAINT "MKORT_PERS_FK" FOREIGN KEY ("PERSON_ID")
        REFERENCES "PERSON" ("PERSON_ID"),
    CONSTRAINT "MKORT_MKORT_FK" FOREIGN KEY ("MELDEKORT_ID_RELATERT")
        REFERENCES "MELDEKORT" ("MELDEKORT_ID")
);

COMMENT ON COLUMN "MELDEKORT"."MELDEKORT_ID" IS 'Generert Oracle-sekvens som entydig identifiserer posten';
COMMENT ON COLUMN "MELDEKORT"."PERSON_ID" IS 'Referanse til PERSON';
COMMENT ON COLUMN "MELDEKORT"."DATO_INNKOMMET" IS 'Meldedato. Dato når meldekort har blitt mottatt.';
COMMENT ON COLUMN "MELDEKORT"."DATO_UTSENDT" IS 'Dato for utsending av meldekort';
COMMENT ON COLUMN "MELDEKORT"."MKSREFERANSE" IS 'Meldekortinformasjon. Referanse til et scannet meldekort hos AMELDING (arkivnøkkel) dersom papirkort';
COMMENT ON COLUMN "MELDEKORT"."MELDEKORTKODE" IS 'Referanse til MELDEKORTKODE. Refererer til MELDEKORTPERIODEBRUK sammen med AAR og PERIODEKODE. ''DP'' eller ''AT''';
COMMENT ON COLUMN "MELDEKORT"."MKSKORTKODE" IS 'Referanse til MKSKORTTYPE. Lovlige meldekorttyper (elektronisk, papir, manuelt osv)';
COMMENT ON COLUMN "MELDEKORT"."STATUS_ARBEIDET" IS 'Svar på om bruker har arbeidet i perioden';
COMMENT ON COLUMN "MELDEKORT"."STATUS_FERIE" IS 'Svar på om bruker har hatt ferie i perioden';
COMMENT ON COLUMN "MELDEKORT"."STATUS_KURS" IS 'Svar på om bruker har vært i utdanning/tiltak i perioden';
COMMENT ON COLUMN "MELDEKORT"."STATUS_NYTT_MELDEKORT" IS 'Ikke i bruk.';
COMMENT ON COLUMN "MELDEKORT"."STATUS_SYK" IS 'Svar på om bruker har vært syk i perioden';
COMMENT ON COLUMN "MELDEKORT"."STATUS_PERIODESPOERSMAAL" IS 'Svar på om bruker ønsker forskudd på utbetaling for neste periode';
COMMENT ON COLUMN "MELDEKORT"."STATUS_SOEKER_DAGPENGER" IS 'Ikke i bruk. Historisk fra omlegging av meldekortløsning i 2005. Brukes ikke for visning av gamle meldekort';
COMMENT ON COLUMN "MELDEKORT"."STATUS_ANNETFRAVAER_ATTF" IS 'Ikke i bruk. Brukes for visning gamle meldekort fra før omlegging av meldekortløsning i 2005';
COMMENT ON COLUMN "MELDEKORT"."STATUS_ATTFORINGSBISTAND" IS 'Ikke i bruk. Brukes for visning av gamle meldekort fra før omlegging av meldekortløsning i 2005';
COMMENT ON COLUMN "MELDEKORT"."STATUS_ATTFORINGSTILTAK" IS 'Ikke i bruk. Brukes for visning gamle meldekort fra før omlegging av meldekortløsning i 2005';
COMMENT ON COLUMN "MELDEKORT"."AAR" IS 'År. Refererer til MELDEKORTPERIODEBRUK sammen med periodekode og meldekortkode';
COMMENT ON COLUMN "MELDEKORT"."PERIODEKODE" IS 'Periodenummer med ledene null (01-53). Refererer til MELDEKORTPERIODEBRUK sammen med aar og meldekortkode';
COMMENT ON COLUMN "MELDEKORT"."BEREGNINGSTATUSKODE" IS 'Referanse til BEREGNINGSTATUS. Definerer om meldekort er klar for beregning, ferdig beregnet eller feilaktig, etc.';
COMMENT ON COLUMN "MELDEKORT"."STATUS_ANNETFRAVAER" IS 'Svar på om bruker av andre grunner ikke vært arbeidssøker';
COMMENT ON COLUMN "MELDEKORT"."STATUS_FORTSATT_ARBEIDSOKER" IS 'Ønsker bruker fremdeles å få meldekort tilsendt og stå som arbeidssøker?';
COMMENT ON COLUMN "MELDEKORT"."FEIL_PAA_KORT" IS 'Status J/N indikerer om det er funnet feil på meldekortet';
COMMENT ON COLUMN "MELDEKORT"."VEILEDNING" IS 'Ikke i bruk. Historisk fra omlegging av meldekortløsning i 2005. Brukes ikke for visning av gamle meldekort';
COMMENT ON COLUMN "MELDEKORT"."KOMMENTAR" IS 'Saksbehandlers kommentar';
COMMENT ON COLUMN "MELDEKORT"."MELDEGRUPPEKODE" IS 'Referanse til MELDEGRUPPETYPE';
COMMENT ON COLUMN "MELDEKORT"."RETURBREVKODE" IS 'Returkode dersom meldekort returneres til bruker. MKS_BREVTYPE inneholder returkoder. Kan inneholde blank dersom ingen retur';
COMMENT ON COLUMN "MELDEKORT"."AB_POSTKODE" IS 'Sende kortet som A, B eller C-post.';
COMMENT ON COLUMN "MELDEKORT"."MELDEKORT_ID_RELATERT" IS 'Relasjon til opprinnelig meldekort';
COMMENT ON COLUMN "MELDEKORT"."PARTISJON" IS 'Partisjonsnøkkel';
COMMENT ON TABLE "MELDEKORT" IS 'Meldekort for dagpenger og AAP.  Tabellen inneholder meldekort for personer som går på Dagpenger og AAP. Meldekortet gjelder for en 2-ukers periode. Innholdet pr dag finnes i tabellen MELDEKORTDAG.Meldekort kan registrers i Arena av saksbehandler, men de fleste leveres elektronisk.';

--------------------------------------------------------------------------------
-- MELDEKORTDAG
--------------------------------------------------------------------------------
CREATE TABLE "MELDEKORTDAG"
(
    "MELDEKORT_ID"             NUMBER                 NOT NULL,
    "UKENR"                    NUMBER(2, 0)           NOT NULL,
    "DAGNR"                    NUMBER(1, 0)           NOT NULL,
    "STATUS_ARBEIDSDAG"        VARCHAR2(1)            NOT NULL,
    "STATUS_FERIE"             VARCHAR2(1),
    "STATUS_KURS"              VARCHAR2(1)            NOT NULL,
    "STATUS_SYK"               VARCHAR2(1)            NOT NULL,
    "STATUS_ANNETFRAVAER_ATTF" VARCHAR2(1),
    "TIMER_ARBEIDET"           NUMBER(3, 1) DEFAULT 0 NOT NULL,
    "TIMER_ARB_MENS_PERM"      NUMBER(3, 1) DEFAULT 0,
    "REG_USER"                 VARCHAR2(8),
    "REG_DATO"                 DATE,
    "MOD_USER"                 VARCHAR2(8),
    "MOD_DATO"                 DATE,
    "STATUS_ANNETFRAVAER"      VARCHAR2(1),
    "MELDEGRUPPEKODE"          VARCHAR2(5),
    "PARTISJON"                NUMBER(8, 0),
    CONSTRAINT "MKDAG_CK1" CHECK (Ukenr BETWEEN 1 AND 53),
    CONSTRAINT "MKDAG_CK2" CHECK (Dagnr BETWEEN 1 AND 7),
    CONSTRAINT "MKDAG_STATJN_CK" CHECK (status_arbeidsdag in ('J', 'N')),
    CONSTRAINT "MKDAG_STATJN_CK2" CHECK (status_ferie in ('J', 'N')),
    CONSTRAINT "MKDAG_STATJN_CK3" CHECK (status_kurs in ('J', 'N')),
    CONSTRAINT "MKDAG_STATJN_CK4" CHECK (status_syk in ('J', 'N')),
    CONSTRAINT "MKDAG_STATJN_CK5" CHECK (status_annetfravaer_attf in ('J', 'N')),

    CONSTRAINT "MKDAG_MKORT_FK" FOREIGN KEY ("MELDEKORT_ID")
        REFERENCES "MELDEKORT" ("MELDEKORT_ID")
);
ALTER TABLE "MELDEKORTDAG"
    ADD CONSTRAINT "MKDAG_PK" PRIMARY KEY ("MELDEKORT_ID", "UKENR", "DAGNR");

COMMENT ON COLUMN "MELDEKORTDAG"."MELDEKORT_ID" IS 'Referanse til MELDEKORT';
COMMENT ON COLUMN "MELDEKORTDAG"."UKENR" IS 'Ukenr mellom 01 og 53';
COMMENT ON COLUMN "MELDEKORTDAG"."DAGNR" IS 'Dagnr mellom 1 og 7';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_ARBEIDSDAG" IS 'Satt hvis TIMER_ARBEIDET > 0';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_FERIE" IS 'Svar på om bruker har hatt ferie';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_KURS" IS 'Svar på om bruker har vært i utdanning/tiltak';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_SYK" IS 'Svar på om bruker har vært syk';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_ANNETFRAVAER_ATTF" IS 'Ikke i bruk. Brukes for visning gamle meldekort fra før omlegging av meldekortløsning i 2005';
COMMENT ON COLUMN "MELDEKORTDAG"."TIMER_ARBEIDET" IS 'Totalt antall timer arbeidet på dagen (egne og annen arbeidsgiver).';
COMMENT ON COLUMN "MELDEKORTDAG"."TIMER_ARB_MENS_PERM" IS 'Ikke i bruk. Antall timer en permittert person har arbeidet hos annen arbeidsgiver.';
COMMENT ON COLUMN "MELDEKORTDAG"."STATUS_ANNETFRAVAER" IS 'Svar på om bruker av andre grunner ikke vært arbeidssøker';
COMMENT ON COLUMN "MELDEKORTDAG"."MELDEGRUPPEKODE" IS 'Referanse til MELDEGRUPPETYPE';
COMMENT ON COLUMN "MELDEKORTDAG"."PARTISJON" IS 'Partisjonsnøkkel';
COMMENT ON TABLE "MELDEKORTDAG" IS 'Opplysninger for et meldekort pr dag i en uke i en meldekortperiode.Det vil ligge opplysninger om hvor mange timer/dager som bruker har arbeidet, vært sy, har annet fravær osv.';

--------------------------------------------------------------------------------
-- MELDEKORTPERIODE
--------------------------------------------------------------------------------
CREATE TABLE "MELDEKORTPERIODE"
(
    "AAR"         NUMBER(4, 0) NOT NULL,
    "PERIODEKODE" VARCHAR2(2)  NOT NULL,
    "UKENR_UKE1"  NUMBER(2, 0) NOT NULL,
    "UKENR_UKE2"  NUMBER(2, 0) NOT NULL,
    "DATO_FRA"    DATE         NOT NULL,
    "DATO_TIL"    DATE         NOT NULL,
    CONSTRAINT "MKORTPER_PK" PRIMARY KEY ("AAR", "PERIODEKODE")
);

COMMENT ON COLUMN "MELDEKORTPERIODE"."AAR" IS 'År';
COMMENT ON COLUMN "MELDEKORTPERIODE"."PERIODEKODE" IS 'Periodenummer med ledene null (01-53)';
COMMENT ON COLUMN "MELDEKORTPERIODE"."UKENR_UKE1" IS 'Ukenr for uke 1 i perioden';
COMMENT ON COLUMN "MELDEKORTPERIODE"."UKENR_UKE2" IS 'Ukenr for uke 2 i perioden';
COMMENT ON COLUMN "MELDEKORTPERIODE"."DATO_FRA" IS 'Fra-dato i gyldighetsperiode';
COMMENT ON COLUMN "MELDEKORTPERIODE"."DATO_TIL" IS 'Til-dato i gyldighetsperiode';
COMMENT ON TABLE "MELDEKORTPERIODE" IS 'Definerer periode på to uker som meldekort gjelder for. Både hvilke uker og hvilke datoer som inngår i en periode. Ytterligere egenskaper til perioden finnes i MELDEKORTPERIODEBRUK.';

--------------------------------------------------------------------------------
-- SAKSTATUS
--------------------------------------------------------------------------------
CREATE TABLE "SAKSTATUS"
(
    "SAKSTATUSKODE" VARCHAR(5)             NOT NULL,
    "SAKSTATUSNAVN" VARCHAR(30)            NOT NULL,
    "FLYTTES_JN"    VARCHAR(1) DEFAULT 'J' NOT NULL,
    CONSTRAINT "SAKSTAT_PK" PRIMARY KEY ("SAKSTATUSKODE")
);

--------------------------------------------------------------------------------
-- SAKSTYPE
--------------------------------------------------------------------------------
CREATE TABLE "SAKSTYPE"
(
    "SAKSKODE"              VARCHAR(10)                          NOT NULL,
    "SAKSTYPENAVN"          VARCHAR(30)                          NOT NULL,
    "ARKIVNOKKEL"           VARCHAR(10),
    "LUKKES_JN"             VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "ANT_DAGER_FOER_LUKK"   INT,
    "HISTORISERES_JN"       VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "ANT_DAGER_FOER_HIST"   INT,
    "FLYTTES_JN"            VARCHAR(1) DEFAULT 'J'               NOT NULL,
    "SPESIAL_FLYTTES_JN"    VARCHAR(1) DEFAULT 'J'               NOT NULL,
    "LOGG_FLYTT_OPPGAVE_JN" VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "KORTNAVN"              VARCHAR(10),
    "TEMASAK_JN"            VARCHAR(1) DEFAULT 'J'               NOT NULL,
    "TEMANAVN"              VARCHAR(30),
    "EKSTERN_JN"            VARCHAR(1) DEFAULT 'J'               NOT NULL,
    "FEILUTBETALING_JN"     VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "OPPRETT_MANUELT_JN"    VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "DATO_GYLDIG_FRA"       DATE       DEFAULT DATE '2000-01-01' NOT NULL,
    "DATO_GYLDIG_TIL"       DATE,
    "PROSESSGRUPPE"         VARCHAR(50),
    "KLAGE_SENDES"          VARCHAR(100),
    "GJELDERKODE"           VARCHAR(10),
    "BRUK_FULLMEKTIG_BREV"  VARCHAR(1) DEFAULT 'N'               NOT NULL,
    CONSTRAINT "SAKSTYP_PK" PRIMARY KEY ("SAKSKODE"),
    CONSTRAINT "SAKSTYP_CK6" CHECK (ekstern_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK5" CHECK (temasak_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK8" CHECK (opprett_manuelt_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK7" CHECK (feilutbetaling_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK" CHECK (lukkes_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK2" CHECK (historiseres_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK3" CHECK (spesial_flyttes_jn IN ('J', 'N')),
    CONSTRAINT "SAKSTYP_CK4" CHECK (logg_flytt_oppgave_jn IN ('J', 'N')),
    CONSTRAINT "SAKTYPE_CK" CHECK (BRUK_FULLMEKTIG_BREV IN ('N', 'S', 'V'))
);

--------------------------------------------------------------------------------
-- RETTIGHETTYPE
--------------------------------------------------------------------------------
CREATE TABLE "RETTIGHETTYPE"
(
    "RETTIGHETKODE"             VARCHAR(10)                          NOT NULL,
    "RETTIGHETNAVN"             VARCHAR(40)                          NOT NULL,
    "DATO_GYLDIG_FRA"           DATE       DEFAULT DATE '2000-01-01' NOT NULL,
    "DATO_GYLDIG_TIL"           DATE,
    "SAKSKODE"                  VARCHAR(10)                          NOT NULL,
    "RETTIGHETSKLASSEKODE"      VARCHAR(10)                          NULL,
    "BELOPKODE"                 VARCHAR(5),
    "RANGNR"                    INT,
    "TRANSAKSJONSKODE"          VARCHAR(5)                           NULL,
    "REG_DATO"                  DATE,
    "REG_USER"                  VARCHAR(8),
    "MOD_DATO"                  DATE,
    "MOD_USER"                  VARCHAR(8),
    "STATUS_KONTERBAR"          VARCHAR(1)                           NOT NULL,
    "TRANSAKSJONSKODE_FORSKUDD" VARCHAR(5),
    "RETTIGHETNAVN_KORT"        VARCHAR(20),
    "FORSKUDD_BETPLAN"          VARCHAR(1),
    "SATSVALG"                  VARCHAR(10),
    "STATUS_TILTAK"             VARCHAR(1),
    "STATUS_START_VEDTAK"       VARCHAR(1),
    "BILAG_KREVES_JN"           VARCHAR(1),
    "BETPLAN_JN"                VARCHAR(1) DEFAULT 'N'               NOT NULL,
    "GJELDERKODE"               VARCHAR(10),
    CONSTRAINT "RETTYP_PK" PRIMARY KEY ("RETTIGHETKODE"),
    CONSTRAINT "RETTYPE_CK" CHECK (status_konterbar IN ('J', 'N')),
    CONSTRAINT "RETTYP_CK2" CHECK ((status_konterbar = 'N' AND belopkode IS NULL) OR (status_konterbar = 'J')),
    CONSTRAINT "RETTYP_CK3" CHECK (forskudd_betplan IN ('J', 'N')),
    CONSTRAINT "RETTYP_CK4" CHECK (SATSVALG IN ('SATS', 'FAKTISK', 'VALGBAR')),
    CONSTRAINT "RETTYP_CK5" CHECK (status_tiltak = 'J'),
    CONSTRAINT "RETTYP_CK6" CHECK (status_start_vedtak IN ('J', 'N')),
    CONSTRAINT "RETTYP_CK7" CHECK (bilag_kreves_jn IN ('J', 'N')),
    CONSTRAINT "RETTYP_CK8" CHECK (betplan_jn IN ('J', 'N')),
    CONSTRAINT "RETTYP_SAKSTYP_FK" FOREIGN KEY ("SAKSKODE") REFERENCES "SAKSTYPE" ("SAKSKODE")
);

CREATE UNIQUE INDEX "RETTYP_UK" ON "RETTIGHETTYPE" ("RETTIGHETNAVN");
ALTER TABLE "RETTIGHETTYPE"
    ADD CONSTRAINT "RETTYP_UK" UNIQUE ("RETTIGHETNAVN");

--------------------------------------------------------------------------------
-- UTFALLTYPE
--------------------------------------------------------------------------------
CREATE TABLE "UTFALLTYPE"
(
    "UTFALLKODE"  VARCHAR(10)  NOT NULL,
    "UTFALLNAVN"  VARCHAR(30)  NOT NULL,
    "UTFALLTEKST" VARCHAR(255) NOT NULL,
    CONSTRAINT "UTFTYP_PK" PRIMARY KEY ("UTFALLKODE")
);

--------------------------------------------------------------------------------
-- VEDTAKSTATUS
--------------------------------------------------------------------------------
CREATE TABLE "VEDTAKSTATUS"
(
    "VEDTAKSTATUSKODE" VARCHAR(5)  NOT NULL,
    "VEDTAKSTATUSNAVN" VARCHAR(30) NOT NULL,
    "BESKRIVELSE"      VARCHAR(255),
    CONSTRAINT "VEDSTAT_PK" PRIMARY KEY ("VEDTAKSTATUSKODE")
);

--------------------------------------------------------------------------------
-- VEDTAKTYPE
--------------------------------------------------------------------------------
CREATE TABLE "VEDTAKTYPE"
(
    "VEDTAKTYPEKODE"           VARCHAR(10) NOT NULL,
    "VEDTAKTYPENAVN"           VARCHAR(30) NOT NULL,
    "BESKRIVELSE"              VARCHAR(255),
    "DATO_FRA"                 DATE        NOT NULL,
    "DATO_TIL"                 DATE        NOT NULL,
    "REG_USER"                 VARCHAR(8),
    "REG_DATO"                 DATE,
    "MOD_USER"                 VARCHAR(8),
    "MOD_DATO"                 DATE,
    "AVSNITTLISTEKODE_VALGFRI" VARCHAR(20),
    CONSTRAINT "VEDTYP_PK" PRIMARY KEY ("VEDTAKTYPEKODE")
);

--------------------------------------------------------------------------------
-- SAK
--------------------------------------------------------------------------------
CREATE TABLE "SAK"
(
    "SAK_ID"               BIGINT                                              NOT NULL,
    "SAKSKODE"             VARCHAR(10) DEFAULT 'INAKT'                         NOT NULL,
    "REG_DATO"             DATE,
    "REG_USER"             VARCHAR(8),
    "MOD_DATO"             DATE,
    "MOD_USER"             VARCHAR(8),
    "TABELLNAVNALIAS"      VARCHAR(10)                                         NOT NULL,
    "OBJEKT_ID"            BIGINT,
    "AAR"                  INT         DEFAULT EXTRACT(YEAR FROM CURRENT_DATE) NOT NULL,
    "LOPENRSAK"            INT                                                 NOT NULL,
    "DATO_AVSLUTTET"       DATE,
    "SAKSTATUSKODE"        VARCHAR(5)                                          NOT NULL,
    "ARKIVNOKKEL"          VARCHAR(7),
    "AETATENHET_ARKIV"     VARCHAR(8),
    "ARKIVHENVISNING"      VARCHAR(255),
    "BRUKERID_ANSVARLIG"   VARCHAR(8),
    "AETATENHET_ANSVARLIG" VARCHAR(8),
    "OBJEKT_KODE"          VARCHAR(10),
    "STATUS_ENDRET"        DATE,
    "PARTISJON"            INT,
    "ER_UTLAND"            VARCHAR(1)  DEFAULT 'N'                             NOT NULL,
    CONSTRAINT "SAK_CK" CHECK (ER_UTLAND IN ('N', 'J')),
    CONSTRAINT "SAK_SAKSTAT_FK" FOREIGN KEY ("SAKSTATUSKODE") REFERENCES "SAKSTATUS" ("SAKSTATUSKODE"),
    CONSTRAINT "SAK_SAKSTYP_FK" FOREIGN KEY ("SAKSKODE") REFERENCES "SAKSTYPE" ("SAKSKODE"),
    CONSTRAINT "SAK_PK" PRIMARY KEY ("SAK_ID"),
    CONSTRAINT "SAK_UK" UNIQUE ("AAR", "LOPENRSAK")
);

--------------------------------------------------------------------------------
-- SAKSRELASJON
--------------------------------------------------------------------------------
CREATE TABLE "SAKSRELASJON"
(
    "SAKSRELASJON_ID"       BIGINT NOT NULL,
    "RELASJONSKODE"         VARCHAR(5),
    "SAK_ID"                BIGINT NOT NULL,
    "EKSTERNENHET_ID"       BIGINT,
    "EKSTERNSAKBETEGNELSE"  VARCHAR(30),
    "SAK_ID_INTERNRELATERT" BIGINT,
    "REG_DATO"              DATE,
    "REG_USER"              VARCHAR(8),
    "MOD_DATO"              DATE,
    "MOD_USER"              VARCHAR(8),
    "PARTISJON"             INT,
    CONSTRAINT "SAKREL_SAK_FK" FOREIGN KEY ("SAK_ID") REFERENCES "SAK" ("SAK_ID"),
    CONSTRAINT "SAKREL_SAK_FK2" FOREIGN KEY ("SAK_ID_INTERNRELATERT") REFERENCES "SAK" ("SAK_ID"),
    CONSTRAINT "SAKREL_PK" PRIMARY KEY ("SAKSRELASJON_ID")
);

--------------------------------------------------------------------------------
-- VEDTAK
--------------------------------------------------------------------------------
CREATE TABLE "VEDTAK"
(
    "VEDTAK_ID"              BIGINT                  NOT NULL,
    "SAK_ID"                 BIGINT                  NOT NULL,
    "VEDTAKSTATUSKODE"       VARCHAR(5)              NOT NULL,
    "VEDTAKTYPEKODE"         VARCHAR(10)             NOT NULL,
    "REG_DATO"               DATE,
    "REG_USER"               VARCHAR(8),
    "MOD_DATO"               DATE,
    "MOD_USER"               VARCHAR(8),
    "UTFALLKODE"             VARCHAR(10),
    "BEGRUNNELSE"            VARCHAR(4000),
    "BRUKERID_ANSVARLIG"     VARCHAR(8),
    "AETATENHET_BEHANDLER"   VARCHAR(8)              NOT NULL,
    "AAR"                    INT        DEFAULT 2000 NOT NULL,
    "LOPENRSAK"              INT                     NOT NULL,
    "LOPENRVEDTAK"           INT                     NOT NULL,
    "RETTIGHETKODE"          VARCHAR(10)             NOT NULL,
    "AKTFASEKODE"            VARCHAR(10)             NOT NULL,
    "BREV_ID"                BIGINT,
    "TOTALBELOP"             DECIMAL(8, 2),
    "DATO_MOTTATT"           DATE                    NOT NULL,
    "VEDTAK_ID_RELATERT"     BIGINT,
    "AVSNITTLISTEKODE_VALGT" VARCHAR(20),
    "HANDLINGSPLAN_ID"       BIGINT,
    "PERSON_ID"              BIGINT,
    "BRUKERID_BESLUTTER"     VARCHAR(8),
    "STATUS_SENSITIV"        VARCHAR(1),
    "VEDLEGG_BETPLAN"        VARCHAR(1),
    "PARTISJON"              INT,
    "OPPSUMMERING_SB2"       VARCHAR(4000),
    "DATO_UTFORT_DEL1"       DATE,
    "DATO_UTFORT_DEL2"       DATE,
    "OVERFORT_NAVI"          VARCHAR(1),
    "FRA_DATO"               DATE,
    "TIL_DATO"               DATE,
    "SF_OPPFOLGING_ID"       BIGINT,
    "STATUS_SOSIALDATA"      VARCHAR(1) DEFAULT 'N'  NOT NULL,
    "KONTOR_SOSIALDATA"      VARCHAR(8),
    "TEKSTVARIANTKODE"       VARCHAR(20),
    "VALGT_BESLUTTER"        VARCHAR(8),
    "TEKNISK_VEDTAK"         VARCHAR(1),
    "DATO_INNSTILT"          DATE,
    "ER_UTLAND"              VARCHAR(1) DEFAULT 'N'  NOT NULL,
    CONSTRAINT "VEDTAK_PK" PRIMARY KEY ("VEDTAK_ID"),
    CONSTRAINT "VEDTAK_CK3" CHECK (overfort_navi IN ('J')),
    CONSTRAINT "VEDTAK_CK" CHECK (status_sensitiv IN ('J', 'N')),
    CONSTRAINT "VEDTAK_CK2" CHECK (vedlegg_betplan IN ('J', 'N')),
    CONSTRAINT "VEDTAK_CKTEK" CHECK (TEKNISK_VEDTAK IN ('J', 'N')),
    CONSTRAINT "VEDTAK_CK_1" CHECK (ER_UTLAND IN ('J', 'N')),
    CONSTRAINT "VEDTAK_SAK_FK" FOREIGN KEY ("SAK_ID") REFERENCES "SAK" ("SAK_ID"),
    CONSTRAINT "VEDTAK_SAK_FK2" FOREIGN KEY ("AAR", "LOPENRSAK") REFERENCES "SAK" ("AAR", "LOPENRSAK"),
    CONSTRAINT "VEDTAK_PERS_FK" FOREIGN KEY ("PERSON_ID") REFERENCES "PERSON" ("PERSON_ID"),
    CONSTRAINT "VEDTAK_UTFTYP_FK" FOREIGN KEY ("UTFALLKODE") REFERENCES "UTFALLTYPE" ("UTFALLKODE"),
    CONSTRAINT "VEDTAK_VEDSTAT_FK" FOREIGN KEY ("VEDTAKSTATUSKODE") REFERENCES "VEDTAKSTATUS" ("VEDTAKSTATUSKODE"),
    CONSTRAINT "VEDTAK_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID_RELATERT") REFERENCES "VEDTAK" ("VEDTAK_ID"),
    CONSTRAINT "VEDTAK_VEDTYP_FK" FOREIGN KEY ("VEDTAKTYPEKODE") REFERENCES "VEDTAKTYPE" ("VEDTAKTYPEKODE"),
    CONSTRAINT "VEDTAK_BREVVGRP_FK" FOREIGN KEY ("TEKSTVARIANTKODE") REFERENCES "BREV_TEKSTVARIANT" ("TEKSTVARIANTKODE"),
    CONSTRAINT "VEDTAK_UK" UNIQUE ("AAR", "LOPENRSAK", "LOPENRVEDTAK")
);

--------------------------------------------------------------------------------
-- POSTERING
--------------------------------------------------------------------------------
CREATE TABLE "POSTERING"
(
    "POSTERING_ID"                NUMBER        NOT NULL,
    "BELOP"                       NUMBER(12, 2) NOT NULL,
    "BELOPKODE"                   VARCHAR2(5)   NOT NULL,
    "DATO_PERIODE_FRA"            DATE          NOT NULL,
    "DATO_PERIODE_TIL"            DATE          NOT NULL,
    "DATO_POSTERT"                DATE          NOT NULL,
    "EKSTERNENHET_ID_ALTMOTTAKER" NUMBER,
    "AAR"                         NUMBER(4, 0)  NOT NULL,
    "PERSON_ID"                   NUMBER        NOT NULL,
    "POSTERINGSATS"               NUMBER(8, 2),
    "POSTERINGTYPEKODE"           VARCHAR2(5)   NOT NULL,
    "TRANSAKSJONSKODE"            VARCHAR2(5)   NOT NULL,
    "ANTALL"                      NUMBER(14, 4),
    "MELDINGKODE"                 VARCHAR2(10),
    "REG_DATO"                    DATE,
    "REG_USER"                    VARCHAR2(8),
    "MOD_DATO"                    DATE,
    "MOD_USER"                    VARCHAR2(8),
    "DATO_GRUNNLAG"               DATE          NOT NULL,
    "VEDTAK_ID"                   NUMBER        NOT NULL,
    "ARTKODE"                     VARCHAR2(5)   NOT NULL,
    "PROSJEKTNUMMER"              VARCHAR2(4),
    "KAPITTEL"                    VARCHAR2(4)   NOT NULL,
    "POST"                        VARCHAR2(2)   NOT NULL,
    "UNDERPOST"                   VARCHAR2(3)   NOT NULL,
    "KONTOSTEDKODE"               VARCHAR2(5),
    "MELDEKORT_ID"                NUMBER,
    "TRANSAKSJONSTEKST"           VARCHAR2(60),
    "BRUKER_ID_SAKSBEHANDLER"     VARCHAR2(8)   NOT NULL,
    "AETATENHET_ANSVARLIG"        VARCHAR2(8)   NOT NULL,
    "TABELLNAVNALIAS_KILDE"       VARCHAR2(10),
    "OBJEKT_ID_KILDE"             NUMBER(20, 0),
    "PARTISJON"                   NUMBER(8, 0),
    CONSTRAINT "POSTER_MKORT_FK" FOREIGN KEY ("MELDEKORT_ID")
        REFERENCES "MELDEKORT" ("MELDEKORT_ID"),
    CONSTRAINT "POSTER_PERS_FK" FOREIGN KEY ("PERSON_ID")
        REFERENCES "PERSON" ("PERSON_ID"),
    CONSTRAINT "POSTER_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID")
        REFERENCES "VEDTAK" ("VEDTAK_ID")
);
CREATE UNIQUE INDEX "POSTER_PK" ON "POSTERING" ("POSTERING_ID", "POSTERINGTYPEKODE");
ALTER TABLE "POSTERING"
    ADD CONSTRAINT "POSTER_PK" PRIMARY KEY ("POSTERING_ID", "POSTERINGTYPEKODE");

COMMENT ON COLUMN "POSTERING"."POSTERING_ID" IS 'Generert Oracle-sekvens som entydig identifiserer posten';
COMMENT ON COLUMN "POSTERING"."BELOP" IS 'Beløp';
COMMENT ON COLUMN "POSTERING"."BELOPKODE" IS 'Referanse til BELOPTYPE';
COMMENT ON COLUMN "POSTERING"."DATO_PERIODE_FRA" IS 'Dato periode fra';
COMMENT ON COLUMN "POSTERING"."DATO_PERIODE_TIL" IS 'Dato periode til';
COMMENT ON COLUMN "POSTERING"."DATO_POSTERT" IS 'Dato postert';
COMMENT ON COLUMN "POSTERING"."EKSTERNENHET_ID_ALTMOTTAKER" IS 'Referanse til BETALINGMOTTAKER';
COMMENT ON COLUMN "POSTERING"."AAR" IS 'År';
COMMENT ON COLUMN "POSTERING"."PERSON_ID" IS 'Referanse til PERSON';
COMMENT ON COLUMN "POSTERING"."POSTERINGSATS" IS 'Posteringsats';
COMMENT ON COLUMN "POSTERING"."POSTERINGTYPEKODE" IS 'Kode som entydig identifiserer posteringstype';
COMMENT ON COLUMN "POSTERING"."TRANSAKSJONSKODE" IS 'Referanse til TRANSAKSJONTYPE';
COMMENT ON COLUMN "POSTERING"."ANTALL" IS 'Antall dager';
COMMENT ON COLUMN "POSTERING"."MELDINGKODE" IS 'Referanse til MELDINGTYPE';
COMMENT ON COLUMN "POSTERING"."REG_DATO" IS 'Dato opprettet';
COMMENT ON COLUMN "POSTERING"."REG_USER" IS 'Oracle brukerident som opprettet posten';
COMMENT ON COLUMN "POSTERING"."MOD_DATO" IS 'Dato sist modifisert';
COMMENT ON COLUMN "POSTERING"."MOD_USER" IS 'Oracle brukerident som sist modifiserte posten';
COMMENT ON COLUMN "POSTERING"."DATO_GRUNNLAG" IS 'Dato grunnlag';
COMMENT ON COLUMN "POSTERING"."VEDTAK_ID" IS 'Referanse til VEDTAK';
COMMENT ON COLUMN "POSTERING"."ARTKODE" IS 'Referanse til ART. Konteringsart';
COMMENT ON COLUMN "POSTERING"."PROSJEKTNUMMER" IS 'Ikke i bruk';
COMMENT ON COLUMN "POSTERING"."KAPITTEL" IS 'Kapittel i statsregnskapet';
COMMENT ON COLUMN "POSTERING"."POST" IS 'Angir post i statsregnskapet';
COMMENT ON COLUMN "POSTERING"."UNDERPOST" IS 'Angir underpost i statsregnskapet';
COMMENT ON COLUMN "POSTERING"."KONTOSTEDKODE" IS 'Referanse til KONTOSTED. Kontosted for en kontering (kontostreng + sted+aar)';
COMMENT ON COLUMN "POSTERING"."MELDEKORT_ID" IS 'Referanse til MELDEKORT. Inkludert av hensyn til utbetalings/posteringshistorikk med referanse til anmerkninger som refererer til meldekort.';
COMMENT ON COLUMN "POSTERING"."TRANSAKSJONSTEKST" IS 'Tekst som beskriver transaksjonen';
COMMENT ON COLUMN "POSTERING"."BRUKER_ID_SAKSBEHANDLER" IS 'ID til ORACLE-brukerident som har lagt inn postering';
COMMENT ON COLUMN "POSTERING"."AETATENHET_ANSVARLIG" IS 'Referanse til ORGUNITINSTANCE';
COMMENT ON COLUMN "POSTERING"."TABELLNAVNALIAS_KILDE" IS 'Peker på tabellen som er opphav til utbetalingen';
COMMENT ON COLUMN "POSTERING"."OBJEKT_ID_KILDE" IS 'Peker på en forekomst i tabellen angitt i Tabellnavnalias_kilde  som er opphav til utbetalingen';
COMMENT ON COLUMN "POSTERING"."PARTISJON" IS 'Partisjonsnøkkel';
COMMENT ON TABLE "POSTERING" IS 'Alle posteringer som er sendt til forsystemet';

--------------------------------------------------------------------------------
-- SIM_UTBETALINGSGRUNNLAG
--------------------------------------------------------------------------------
CREATE TABLE "SIM_UTBETALINGSGRUNNLAG"
(
    "SIM_POSTERING_ID"            BIGINT     NOT NULL,
    "BELOP"                       DECIMAL(12, 2),
    "BELOPKODE"                   VARCHAR(5) NOT NULL,
    "EKSTERNENHET_ID_ALTMOTTAKER" BIGINT,
    "AAR"                         INT        NOT NULL,
    "DATO_PERIODE_FRA"            DATE       NOT NULL,
    "PERSON_ID"                   BIGINT     NOT NULL,
    "POSTERINGTYPEKODE"           VARCHAR(5),
    "TRANSAKSJONSKODE"            VARCHAR(5) NOT NULL,
    "ANTALL"                      DECIMAL(14, 4),
    "POSTERINGSATS"               DECIMAL(8, 2),
    "MELDINGKODE"                 VARCHAR(10),
    "ARTKODE"                     VARCHAR(5) NOT NULL,
    "DATO_PERIODE_TIL"            DATE       NOT NULL,
    "REG_DATO"                    DATE,
    "KAPITTEL"                    VARCHAR(4),
    "MOD_DATO"                    DATE,
    "REG_USER"                    VARCHAR(8),
    "MOD_USER"                    VARCHAR(8),
    "VEDTAK_ID"                   BIGINT,
    "DATO_GRUNNLAG"               DATE       NOT NULL,
    "POST"                        VARCHAR(2),
    "PROSJEKTNUMMER"              VARCHAR(4),
    "SIM_MELDEKORT_ID"            BIGINT,
    "TRANSAKSJONSTEKST"           VARCHAR(60),
    "UNDERPOST"                   VARCHAR(3),
    "KONTOSTEDKODE"               VARCHAR(5) NOT NULL,
    "STATUS_MANUELL"              VARCHAR(1),
    "KOMMENTAR"                   VARCHAR(2000),
    "VEDTAK_ID_FEILUTBET"         BIGINT     NOT NULL,
    "TABELLNAVNALIAS_KILDE"       VARCHAR(10),
    "OBJEKT_ID_KILDE"             BIGINT,
    "PARTISJON"                   INT,
    "BELOP_SATT_MANUELT"          VARCHAR(1),
    "POSTERING_ID"                BIGINT,
    CONSTRAINT "SUTBETGR_CK" CHECK (STATUS_MANUELL IN ('J', 'N')),
    CONSTRAINT "SUTBETGR_CK2" CHECK (BELOP_SATT_MANUELT IN ('J', 'N')),
    CONSTRAINT "SUTBETGR_PERS_FK" FOREIGN KEY ("PERSON_ID") REFERENCES "PERSON" ("PERSON_ID"),
    CONSTRAINT "SUTBETGR_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID") REFERENCES "VEDTAK" ("VEDTAK_ID"),
    CONSTRAINT "SUTBETGR_VEDTAK_FK2" FOREIGN KEY ("VEDTAK_ID_FEILUTBET") REFERENCES "VEDTAK" ("VEDTAK_ID"),
    CONSTRAINT "SUTBETGR_PK" PRIMARY KEY ("SIM_POSTERING_ID")
);

--------------------------------------------------------------------------------
-- SPESIALUTBETALING
--------------------------------------------------------------------------------
CREATE TABLE "SPESIALUTBETALING"
(
    "SPESUTBETALING_ID"           BIGINT      NOT NULL,
    "PERSON_ID"                   BIGINT      NOT NULL,
    "VEDTAK_ID"                   BIGINT,
    "LOPENR"                      INT,
    "BRUKER_ID_SAKSBEHANDLER"     VARCHAR(8)  NOT NULL,
    "BRUKER_ID_BESLUTTER"         VARCHAR(8),
    "DATO_UTBETALING"             DATE,
    "BEGRUNNELSE"                 VARCHAR(2000),
    "BELOP"                       DECIMAL(12, 2),
    "BELOPKODE"                   VARCHAR(5)  NOT NULL,
    "RETTIGHETKODE"               VARCHAR(10) NOT NULL,
    "AKTFASEKODE"                 VARCHAR(10) NOT NULL,
    "VEDTAKSTATUSKODE"            VARCHAR(5)  NOT NULL,
    "POSTERINGTYPEKODE"           VARCHAR(5)  NOT NULL,
    "REFERANSE_TOTAL"             VARCHAR(255),
    "DATO_FRA"                    DATE,
    "DATO_TIL"                    DATE,
    "REG_DATO"                    DATE,
    "REG_USER"                    VARCHAR(8),
    "MOD_DATO"                    DATE,
    "MOD_USER"                    VARCHAR(8),
    "EKSTERNENHET_ID_ALTMOTTAKER" BIGINT,
    "FERIEGRUNNLAG"               DECIMAL(12, 2),
    "FERIEGRUNNLAGKODE"           VARCHAR(10),
    "ORDINAER_YTELSE"             VARCHAR(1),
    "REFERANSE_BILAG"             VARCHAR(25),
    "STATUS_BILAG"                VARCHAR(1),
    "STATUS_ANVIS_BILAG"          VARCHAR(1),
    "PARTISJON"                   INT,
    "VALGT_UTBET_TYPE"            VARCHAR(20),
    "KATEGORI"                    VARCHAR(50),
    CONSTRAINT "SPESBET_CK" CHECK (ORDINAER_YTELSE IN ('J')),
    CONSTRAINT "SPESBET_CK2" CHECK (STATUS_BILAG IN ('J', 'N')),
    CONSTRAINT "SPESBET_CK3" CHECK (STATUS_ANVIS_BILAG IN ('J', 'N')),
    CONSTRAINT "SPESBET_PERS_FK" FOREIGN KEY ("PERSON_ID") REFERENCES "PERSON" ("PERSON_ID"),
    CONSTRAINT "SPESBET_VEDST_FK" FOREIGN KEY ("VEDTAKSTATUSKODE") REFERENCES "VEDTAKSTATUS" ("VEDTAKSTATUSKODE"),
    CONSTRAINT "SPESBET_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID") REFERENCES "VEDTAK" ("VEDTAK_ID"),
    CONSTRAINT "SPESBET_PK" PRIMARY KEY ("SPESUTBETALING_ID"),
    CONSTRAINT "SPESBET_UK" UNIQUE ("LOPENR", "VEDTAK_ID")
);

--------------------------------------------------------------------------------
-- ANMERKNINGTYPE
--------------------------------------------------------------------------------
CREATE TABLE "ANMERKNINGTYPE"
(
    "ANMERKNINGKODE"   VARCHAR(5)   NOT NULL,
    "ANMERKNINGNAVN"   VARCHAR(100) NOT NULL,
    "BESKRIVELSE"      VARCHAR(255),
    "HENDELSETYPEKODE" VARCHAR(7)   NOT NULL,
    CONSTRAINT "ANMTYP_PK" PRIMARY KEY ("ANMERKNINGKODE")
);

COMMENT ON COLUMN "ANMERKNINGTYPE"."ANMERKNINGKODE" IS 'Entydig kode for anmerkningtypen';
COMMENT ON COLUMN "ANMERKNINGTYPE"."ANMERKNINGNAVN" IS 'Navn på anmerkningtypen';
COMMENT ON COLUMN "ANMERKNINGTYPE"."BESKRIVELSE" IS 'Beskrivelse av anmerkningtypen (kan inkludere flettefelt for verdien fra ANMERKNING)';
COMMENT ON COLUMN "ANMERKNINGTYPE"."HENDELSETYPEKODE" IS 'Referanse til HENDELSETYPE';
COMMENT ON TABLE "ANMERKNINGTYPE" IS 'Kodetabell for anmerkningtyper';

--------------------------------------------------------------------------------
-- ANMERKNING
--------------------------------------------------------------------------------
CREATE TABLE "ANMERKNING"
(
    "ANMERKNINGKODE"  VARCHAR(5)  NOT NULL,
    "REG_USER"        VARCHAR(8),
    "REG_DATO"        DATE,
    "VERDI"           INT,
    "ANMERKNING_ID"   BIGINT      NOT NULL,
    "TABELLNAVNALIAS" VARCHAR(10) NOT NULL,
    "OBJEKT_ID"       BIGINT      NOT NULL,
    "VEDTAK_ID"       BIGINT,
    "PARTISJON"       INT,
    "MOD_USER"        VARCHAR(8),
    "MOD_DATO"        DATE,
    "VERDI2"          INT,
    CONSTRAINT "ANMERK_ANMTYP_FK" FOREIGN KEY ("ANMERKNINGKODE")
        REFERENCES "ANMERKNINGTYPE" ("ANMERKNINGKODE"),
    CONSTRAINT "ANMERK_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID")
        REFERENCES "VEDTAK" ("VEDTAK_ID")
);
ALTER TABLE "ANMERKNING"
    ADD CONSTRAINT "ANMERK_PK" PRIMARY KEY ("ANMERKNING_ID");

COMMENT ON COLUMN "ANMERKNING"."ANMERKNINGKODE" IS 'Referanse til ANMERKNINGTYPE';
COMMENT ON COLUMN "ANMERKNING"."REG_USER" IS 'Angir hvilken bruker som opprettet raden';
COMMENT ON COLUMN "ANMERKNING"."REG_DATO" IS 'Angir tidspunkt for når raden ble opprettet';
COMMENT ON COLUMN "ANMERKNING"."VERDI" IS 'Flettes inn i subtitusjonsparameter 1 i beskrivelsen i anmerkningtype.';
COMMENT ON COLUMN "ANMERKNING"."ANMERKNING_ID" IS 'Unik ID for anmerkningen';
COMMENT ON COLUMN "ANMERKNING"."TABELLNAVNALIAS" IS 'Angir hva anmerkningen gjelder. Sammen med objekt_id er det en entydig referanse.';
COMMENT ON COLUMN "ANMERKNING"."OBJEKT_ID" IS 'Referanse til det anmerkningen gjelder. Sammen med objekt_id er det en entydig referanse.';
COMMENT ON COLUMN "ANMERKNING"."VEDTAK_ID" IS 'Referanse til VEDTAK';
COMMENT ON COLUMN "ANMERKNING"."PARTISJON" IS 'Angir partisjonsnøkkelen ifbm. Historiseringsbatchen';
COMMENT ON COLUMN "ANMERKNING"."MOD_USER" IS 'Angir hvilken bruker som sist endret raden';
COMMENT ON COLUMN "ANMERKNING"."MOD_DATO" IS 'Angir tidspunkt for når raden sist ble endret';
COMMENT ON COLUMN "ANMERKNING"."VERDI2" IS 'Flettes inn i subtitusjonsparameter 2 i beskrivelsen i anmerkningtype.';
COMMENT ON TABLE "ANMERKNING" IS 'Inneholder alle forskjellige anmerkninger som kan knyttes til (hovedsakelig) meldekort';

--------------------------------------------------------------------------------
-- VEDTAKFAKTATYPE
--------------------------------------------------------------------------------
CREATE TABLE "VEDTAKFAKTATYPE"
(
    "VEDTAKFAKTAKODE"      VARCHAR(10)  NOT NULL,
    "SKJERMBILDETEKST"     VARCHAR(255) NOT NULL,
    "STATUS_KVOTEBRUK"     VARCHAR(1)   NOT NULL,
    "STATUS_OVERSIKT"      VARCHAR(1)   NOT NULL,
    "VEDTAKFAKTANAVN"      VARCHAR(30)  NOT NULL,
    "BESKRIVELSE"          VARCHAR(255),
    "ORACLETYPE"           VARCHAR(10),
    "FELTLENGDE"           INT,
    "AVSNITT_ID_LEDETEKST" BIGINT,
    "REG_DATO"             DATE,
    "REG_USER"             VARCHAR(8),
    "MOD_DATO"             DATE,
    "MOD_USER"             VARCHAR(8),
    CONSTRAINT "VEDFAKTTYP_PK" PRIMARY KEY ("VEDTAKFAKTAKODE"),
    CONSTRAINT "VEDFAKTTYP_STATJN_CK" CHECK (status_kvotebruk IN ('J', 'N')),
    CONSTRAINT "VEDFAKTTYP_STATJN_CK2" CHECK (status_oversikt IN ('J', 'N'))
);

--------------------------------------------------------------------------------
-- VEDTAKFAKTA
--------------------------------------------------------------------------------
CREATE TABLE "VEDTAKFAKTA"
(
    "VEDTAK_ID"       BIGINT      NOT NULL,
    "VEDTAKFAKTAKODE" VARCHAR(10) NOT NULL,
    "VEDTAKVERDI"     VARCHAR(2000),
    "REG_DATO"        DATE,
    "REG_USER"        VARCHAR(8),
    "MOD_DATO"        DATE,
    "MOD_USER"        VARCHAR(8),
    "PERSON_ID"       BIGINT,
    "PARTISJON"       INT,
    CONSTRAINT "VEDFAKT_PK" PRIMARY KEY ("VEDTAK_ID", "VEDTAKFAKTAKODE"),
    CONSTRAINT "VEDFAKT_VEDFTYP_FK" FOREIGN KEY ("VEDTAKFAKTAKODE") REFERENCES "VEDTAKFAKTATYPE" ("VEDTAKFAKTAKODE"),
    CONSTRAINT "VEDFAKT_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID") REFERENCES "VEDTAK" ("VEDTAK_ID")
);

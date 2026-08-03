--------------------------------------------------------------------------------
-- V_OPPGAVE
-- Arena eksponerer oppgaver gjennom denne viewet. I H2 lages den som tabell,
-- siden vi ikke har de underliggende Arena-tabellene tilgjengelig i tester.
--------------------------------------------------------------------------------
CREATE TABLE "V_OPPGAVE"
(
    "PERSON_ID"     NUMBER       NOT NULL,
    "FODSELSNR"     VARCHAR2(11),
    "TASK_ID"       NUMBER       NOT NULL,
    "DESCRIPTION"   VARCHAR2(100),
    "CASECONTEXT"   VARCHAR2(80),
    "DISPLAYNAME"   VARCHAR2(100),
    "DUEDATE"       DATE,
    "ARBEIDSBENK"   VARCHAR2(20),
    "OPPGAVE_ENHET" VARCHAR2(8),
    "NAV_ENHET"     VARCHAR2(8),
    "NOTE"          VARCHAR2(2000)
);

CREATE INDEX "V_OPPGAVE_PERS_FKI" ON "V_OPPGAVE" ("PERSON_ID");


--------------------------------------------------------------------------------
-- V_OPPGAVE
-- Arena eksponerer oppgaver gjennom denne viewet. I H2 lages den som tabell,
-- siden vi ikke har de underliggende Arena-tabellene tilgjengelig i tester.
--------------------------------------------------------------------------------
CREATE TABLE "V_OPPGAVE"
(
    "PERSON_ID"     NUMBER,
    "FODSELSNR"     VARCHAR2(11),
    "TASK_ID"       VARCHAR2(50),
    "DESCRIPTION"   VARCHAR2(2000),
    "CASECONTEXT"   VARCHAR2(255),
    "DISPLAYNAME"   VARCHAR2(255),
    "DUEDATE"       DATE,
    "ARBEIDSBENK"   VARCHAR2(255),
    "OPPGAVE_ENHET" VARCHAR2(10),
    "NAV_ENHET"     VARCHAR2(10),
    "NOTE"          VARCHAR2(2000)
);

CREATE INDEX "V_OPPGAVE_PERS_FKI" ON "V_OPPGAVE" ("PERSON_ID");


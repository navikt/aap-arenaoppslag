--------------------------------------------------------------------------------
-- SAKSOPPLYSNINGTYPE
--------------------------------------------------------------------------------
CREATE TABLE "SAKSOPPLYSNINGTYPE"
(
    "SAKSOPPLYSNINGKODE"  VARCHAR2(20)  NOT NULL,
    "SAKSOPPLYSNINGNAVN"  VARCHAR2(100) NOT NULL,
    "SKJERMBILDETEKST"    VARCHAR2(255),
    "STATUS_REPETERBAR"   VARCHAR2(1)   DEFAULT 'N' NOT NULL,
    CONSTRAINT "SAKSOPPLTYP_PK" PRIMARY KEY ("SAKSOPPLYSNINGKODE"),
    CONSTRAINT "SAKSOPPLTYP_CK" CHECK (STATUS_REPETERBAR IN ('J', 'N'))
);

--------------------------------------------------------------------------------
-- SAKSOPPLYSNING
--------------------------------------------------------------------------------
CREATE TABLE "SAKSOPPLYSNING"
(
    "SAKSOPPLYSNING_ID"  NUMBER       NOT NULL,
    "SAKSOPPLYSNINGKODE" VARCHAR2(20) NOT NULL,
    "VERDI"              VARCHAR2(4000),
    CONSTRAINT "SAKSOPPL_PK" PRIMARY KEY ("SAKSOPPLYSNING_ID"),
    CONSTRAINT "SAKSOPPL_TYPE_FK" FOREIGN KEY ("SAKSOPPLYSNINGKODE")
        REFERENCES "SAKSOPPLYSNINGTYPE" ("SAKSOPPLYSNINGKODE")
);

--------------------------------------------------------------------------------
-- ATTRIBUTTYPE
--------------------------------------------------------------------------------
CREATE TABLE "ATTRIBUTTYPE"
(
    "ATTRIBUTTYPE_ID"    NUMBER        NOT NULL,
    "SAKSOPPLYSNINGKODE" VARCHAR2(20)  NOT NULL,
    "ATTRIBUTTKODE"      VARCHAR2(20)  NOT NULL,
    "SKJERMBILDETEKST"   VARCHAR2(255),
    "FORMATNAVN"         VARCHAR2(50),
    "POSISJON"           NUMBER(5, 0)  NOT NULL,
    CONSTRAINT "ATTRTYP_PK" PRIMARY KEY ("ATTRIBUTTYPE_ID"),
    CONSTRAINT "ATTRTYP_TYPE_FK" FOREIGN KEY ("SAKSOPPLYSNINGKODE")
        REFERENCES "SAKSOPPLYSNINGTYPE" ("SAKSOPPLYSNINGKODE")
);

--------------------------------------------------------------------------------
-- ATTRIBUTT
--------------------------------------------------------------------------------
CREATE TABLE "ATTRIBUTT"
(
    "ATTRIBUTT_ID"          NUMBER       NOT NULL,
    "SAKSOPPLYSNING_ID_EIER" NUMBER      NOT NULL,
    "ATTRIBUTTYPE_ID"        NUMBER      NOT NULL,
    "VERDI"                  VARCHAR2(4000),
    "STATUS_SJEKKET_AV"      VARCHAR2(1),
    CONSTRAINT "ATTR_PK" PRIMARY KEY ("ATTRIBUTT_ID"),
    CONSTRAINT "ATTR_SAKSOPPL_FK" FOREIGN KEY ("SAKSOPPLYSNING_ID_EIER")
        REFERENCES "SAKSOPPLYSNING" ("SAKSOPPLYSNING_ID"),
    CONSTRAINT "ATTR_ATTRTYP_FK" FOREIGN KEY ("ATTRIBUTTYPE_ID")
        REFERENCES "ATTRIBUTTYPE" ("ATTRIBUTTYPE_ID")
);

--------------------------------------------------------------------------------
-- LOV_VEDTAK_SAKSOPPLYSNING
--------------------------------------------------------------------------------
CREATE TABLE "LOV_VEDTAK_SAKSOPPLYSNING"
(
    "VEDTAK_ID"         BIGINT NOT NULL,
    "SAKSOPPLYSNING_ID" NUMBER NOT NULL,
    CONSTRAINT "LOVVS_PK" PRIMARY KEY ("VEDTAK_ID", "SAKSOPPLYSNING_ID"),
    CONSTRAINT "LOVVS_VEDTAK_FK" FOREIGN KEY ("VEDTAK_ID")
        REFERENCES "VEDTAK" ("VEDTAK_ID"),
    CONSTRAINT "LOVVS_SAKSOPPL_FK" FOREIGN KEY ("SAKSOPPLYSNING_ID")
        REFERENCES "SAKSOPPLYSNING" ("SAKSOPPLYSNING_ID")
);


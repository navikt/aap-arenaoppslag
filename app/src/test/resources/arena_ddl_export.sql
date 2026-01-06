-- Oracle syntax for å eksportere DDL til views som om de var tabeller,
-- slik at vi kan kjøre representative create table når vi kjører enhetstester

-- =============================================================
-- Views -> Tables DDL Generator (Oracle -> Oracle) - Simplified
-- Oracle 19c compatible, minimal CTEs, clean types/NOT NULL/PK
-- Source: ARENA_TILGANG_AAP | Target: ARENA_TILGANG_AAP (default)
-- =============================================================

DEFINE SRC_OWNER=ARENA_TILGANG_AAP
DEFINE TGT_OWNER=ARENA_TILGANG_AAP

SET PAGESIZE 0 LONG 100000 LINESIZE 32767 TRIMSPOOL ON TERMOUT OFF FEEDBACK OFF VERIFY OFF

    SPOOL create_tables_from_views_ddl.sql

WITH
-- Views in scope
views_in_scope AS (
    SELECT v.view_name
    FROM all_views v
    WHERE v.owner = UPPER('&SRC_OWNER')
),

-- Column metadata for those views
vcols AS (
    SELECT
        c.table_name         AS view_name,
        c.column_id,
        c.column_name,
        c.nullable,
        c.data_type,
        c.data_length,
        c.char_length,
        c.data_precision,
        c.data_scale
    FROM all_tab_columns c
             JOIN views_in_scope s
                  ON s.view_name = c.table_name
    WHERE c.owner = UPPER('&SRC_OWNER')
),

-- Datatype + NOT NULL formatting (Oracle -> Oracle)
col_defs AS (
    SELECT
        view_name,
        column_id,
        column_name,
        CASE
            WHEN data_type IN ('VARCHAR2','NVARCHAR2','CHAR') THEN
                data_type || '(' || NVL(TO_CHAR(char_length), TO_CHAR(data_length)) || ')'
            WHEN data_type = 'NUMBER' THEN
                CASE
                    WHEN data_precision IS NULL AND data_scale IS NULL THEN 'NUMBER'
                    WHEN data_precision IS NOT NULL AND data_scale IS NULL THEN 'NUMBER('||data_precision||')'
                    ELSE 'NUMBER('||data_precision||','||NVL(data_scale,0)||')'
                    END
            WHEN data_type LIKE 'TIMESTAMP%' THEN 'TIMESTAMP'
            WHEN data_type = 'DATE' THEN 'DATE'
            WHEN data_type IN ('CLOB','NCLOB','BLOB','RAW','LONG RAW','LONG') THEN data_type
            ELSE data_type
            END ||
        CASE WHEN nullable = 'N' THEN ' NOT NULL' ELSE '' END
            AS col_def
    FROM vcols
),

-- Column block per view
ddl_cols AS (
    SELECT
        view_name,
        LISTAGG('  '||column_name||' '||col_def, ','||CHR(10))
        WITHIN GROUP (ORDER BY column_id) AS col_block
    FROM col_defs
    GROUP BY view_name
),

-- Heuristic PK detection (exactly one match of ID or <VIEW>_ID and NOT NULL)
pk_auto AS (
    SELECT c.view_name, c.column_name
    FROM vcols c
    WHERE c.nullable = 'N'
      AND (UPPER(c.column_name) = 'ID'
        OR UPPER(c.column_name) = UPPER(c.view_name) || '_ID')
),
pk_auto_counts AS (
    SELECT view_name, COUNT(*) AS cnt
    FROM pk_auto
    GROUP BY view_name
),
pk_auto_single AS (
    SELECT p.view_name, p.column_name
    FROM pk_auto p
             JOIN pk_auto_counts k
                  ON k.view_name = p.view_name
                      AND k.cnt = 1
),

-- Manual PK overrides/additions (leave empty to rely on auto heuristic)
pk_manual AS (
    -- Example: multiple PK columns for one view
    -- SELECT 'SAK' AS view_name, 'SAK_ID' AS column_name FROM dual UNION ALL
    -- SELECT 'SAK', 'VERSJON' FROM dual
    SELECT CAST(NULL AS VARCHAR2(128)) AS view_name, CAST(NULL AS VARCHAR2(128)) AS column_name FROM dual WHERE 1=0
),

-- Final PK list per view (auto + manual)
pk_map AS (
    SELECT view_name, column_name FROM pk_auto_single
    UNION ALL
    SELECT view_name, column_name FROM pk_manual
),
pk_list AS (
    SELECT view_name,
           LISTAGG(column_name, ', ') WITHIN GROUP (ORDER BY column_name) AS pk_cols
    FROM pk_map
    GROUP BY view_name
)

-- Emit CREATE TABLE DDL
SELECT
--  'PROMPT Creating DDL for &TGT_OWNER.'||d.view_name||CHR(10)||
'CREATE TABLE &TGT_OWNER.'||d.view_name||CHR(10)||
'('||CHR(10)||
d.col_block ||
CASE WHEN p.pk_cols IS NOT NULL THEN ','||CHR(10)||
                                     '  CONSTRAINT PK_'||d.view_name||' PRIMARY KEY ('||p.pk_cols||')'
     ELSE ''
    END ||CHR(10)||
');' AS ddl
FROM ddl_cols d
         LEFT JOIN pk_list p
                   ON p.view_name = d.view_name
ORDER BY d.view_name;


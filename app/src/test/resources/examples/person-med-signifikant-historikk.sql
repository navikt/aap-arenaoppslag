-- Eksempelspørring for å finne ut om en person har nyere historikk i Arena
-- som kan være signifikant for om personen kan tas inn i Kelvin eller ikke.

-- :person_id er en verdi fra person tabellen i Arena
SELECT
    sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    fra_dato,
    til_dato,
    rettighetkode,
    utfallkode
FROM
    vedtak v
WHERE v.person_id = :person_id
  AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
  AND v.rettighetkode = 'AAP'
  AND v.MOD_DATO >= DATE '2020-06-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak (72 mnd)
  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
  AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
  AND (
    (vedtaktypekode IN ('O','E','G') AND (til_dato IS NULL OR til_dato >= DATE '2024-12-17')) -- vanlig tidsbuffer på 18 måneder
        OR
    (vedtaktypekode = 'S' AND NOT EXISTS(select vedtak_id from vedtak vv where
        vv.lopenrvedtak > v.lopenrvedtak and vv.vedtak_id=v.vedtak_id_relatert and vv.vedtaktypekode !='S') AND (fra_dato IS NULL OR fra_dato >= DATE '2024-03-04')) -- ekstra tidsbuffer for Stans, som bare har fra_dato
    )
  AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND (fra_dato IS NOT NULL AND fra_dato <= DATE '2024-12-17')) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle
UNION ALL
SELECT
    sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    fra_dato,
    til_dato,
    rettighetkode,
    utfallkode
FROM
    vedtak v
WHERE v.person_id = :person_id
  AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
  AND v.rettighetkode = 'AA115'
  AND v.MOD_DATO >= DATE '2020-06-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak (72 mnd)
  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
  AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
  AND (
    (vedtaktypekode IN ('O','E','G') AND (til_dato IS NULL OR til_dato >= DATE '2024-12-17')) -- vanlig tidsbuffer på 18 måneder
        OR
    (vedtaktypekode = 'S' AND NOT EXISTS(select vedtak_id from vedtak vv where
        vv.lopenrvedtak > v.lopenrvedtak and vv.vedtak_id=v.vedtak_id_relatert and vv.vedtaktypekode !='S') AND (fra_dato IS NULL OR fra_dato >= DATE '2024-03-04')) -- ekstra tidsbuffer for Stans, som bare har fra_dato
    )
  AND NOT (utfallkode = 'NEI' AND til_dato IS NULL) -- bruker fikk avslag
UNION ALL
-- INNVF er satt for alle klager. Den får alltid en dato-verdi når utfallet av klagen registreres.
-- Dersom den er null, er klagen fortsatt under behandling.
SELECT
    v.sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    CAST(NULL AS DATE)                    AS fra_dato,
    TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
    v.rettighetkode,
    v.utfallkode
FROM
    vedtak v
        JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
WHERE
    v.person_id = :person_id
  AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
  AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
  AND v.MOD_DATO >= DATE '2020-06-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak (72 mnd)
  AND vf.vedtakfaktakode = 'INNVF'
  -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.
  -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
  AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= DATE '2024-12-17' )
  -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger. Ekskluder disse.
  AND NOT ( vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= DATE '2025-12-15' AND v.utfallkode IN ('JA', 'DELVIS' ) )
UNION ALL
SELECT
    v.sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    CAST(NULL AS DATE)                    AS fra_dato,
    CAST(NULL AS DATE)                    AS til_dato,
    v.rettighetkode,
    v.utfallkode
FROM
    vedtak v
        JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
WHERE
    v.person_id = :person_id
  AND (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
  AND rettighetkode = 'ANKE'
  AND v.MOD_DATO >= DATE '2020-06-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak (72 mnd)

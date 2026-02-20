-- Eksempelspørring for å finne ut om en person har nyere historikk i Arena
-- som kan være signifikant for om personen kan tas inn i Kelvin eller ikke.

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
  AND v.rettighetkode IN ('AA115', 'AAP')
  AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak
  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
  AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
  AND (
    (vedtaktypekode IN ('O','E','G') AND (til_dato >= DATE '2024-06-15' OR til_dato IS NULL)) -- vanlig tidsbuffer på 18 måneder
        OR
    (vedtaktypekode = 'S' AND (fra_dato >= DATE '2024-01-01' OR fra_dato IS NULL)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
    )
  AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND fra_dato <= DATE '2024-06-15') -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle

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
  AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak, begrens string-til-dato konvertering
  AND vf.vedtakfaktakode = 'INNVF'
  -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.
  -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
  AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= DATE '2024-06-15' )
  -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger. Ekskluder disse.
  AND NOT ( vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) AND v.utfallkode IN ('JA', 'DELVIS' ) )

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
  AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -72) -- ytelse: unngå å løpe gjennom veldig gamle vedtak

UNION ALL

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
  AND rettighetkode = 'TILBBET'
  AND (v.utfallkode IS NOT NULL AND v.utfallkode != 'AVBRUTT')
  AND v.MOD_DATO >= ADD_MONTHS(TRUNC(SYSDATE), -60) -- ytelse: unngå å løpe gjennom veldig gamle vedtak
  AND vf.vedtakfaktakode = 'INNVF'
  -- Vi regner tilbakebetalinger med null INNVF som åpne, ellers ikke
  AND vf.vedtakverdi IS NULL -- det er ikke satt endelig dato for beslutning på vedtaket

UNION ALL

SELECT
    v.sak_id,
    su.vedtakstatuskode,
    'O'  AS vedtaktypekode,
    su.dato_fra AS fra_dato,
    su.dato_til AS til_dato,
    'SPESIAL' AS rettighetkode,
    v.utfallkode
FROM
    spesialutbetaling su
        JOIN vedtak v ON v.vedtak_id = su.vedtak_id -- for å få sak_id
WHERE
    su.person_id = :person_id
  -- Dersom utbetalingen ikke er datofestet, eller den har skjedd nylig, regner vi saken som åpen, ellers ikke.
  -- Vi bruker en tidsbuffer her i tilfelle det klages på spesialutbetalingen etter at den er utbetalt.
  AND (su.dato_utbetaling IS NULL OR su.dato_utbetaling >= ADD_MONTHS(TRUNC(SYSDATE), -3) )
-- MERK: ingen index i spesialutbetaling på dato_utbetaling eller andre dato-felt, så det går tregt

UNION ALL

SELECT
    v.sak_id,
    v.vedtakstatuskode,
    'O'  AS vedtaktypekode,
    ssu.dato_periode_fra AS fra_dato,
    ssu.dato_periode_til AS til_dato,
    'SIM_SPESIAL' AS rettighetkode,
    v.utfallkode
FROM
    sim_utbetalingsgrunnlag ssu
        JOIN vedtak v ON v.vedtak_id = ssu.vedtak_id
WHERE
    ssu.person_id = :person_id
  -- MERK: ingen index i sim_utbetalingsgrunnlag på mod_dato eller andre datofelt, så blir tregt
  AND ssu.mod_dato >= ADD_MONTHS(TRUNC(SYSDATE), -3) -- ignorer gamle simuleringer som ikke ble noe av
;
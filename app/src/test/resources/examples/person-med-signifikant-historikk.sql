-- Eksempelspørring for å finne ut om en person har nyere historikk i Arena
-- som kan være signifikant for om personen kan tas inn i Kelvin eller ikke.

-- TODO: search-replace '?' med en person_id som finnes i Arena

SELECT sak_id, vedtakstatuskode, vedtaktypekode, fra_dato, til_dato, rettighetkode
FROM
    vedtak v

WHERE v.person_id = ?
  AND v.utfallkode != 'AVBRUTT'
  AND v.rettighetkode IN ('AA115', 'AAP')
  AND v.MOD_DATO >= DATE '2020-01-01' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
  AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL)) -- filtrer ut ugyldiggjorte vedtak
  AND NOT ((fra_dato IS NULL AND til_dato IS NULL) AND vedtakstatuskode NOT IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
  AND (
    (vedtaktypekode IN ('O','E','G') AND (til_dato >= DATE '2024-06-15' OR til_dato IS NULL)) -- vanlig tidsbuffer på 18 måneder
        OR
    (vedtaktypekode = 'S' AND (fra_dato >= DATE '2024-01-01' OR fra_dato IS NULL)) -- ekstra tidsbuffer for Stans, som bare har fra_dato
    )
  AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND fra_dato <= '2024-06-15') -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle

UNION ALL

-- INNVF er satt for alle klager. Den får alltid en dato-verdi når utfallet av klagen registreres.
-- Dersom den er null, er klagen fortsatt under behandling.
SELECT
    v.sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    CAST(NULL AS DATE)                    AS fra_dato,
    TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
    v.rettighetkode
FROM
    vedtak v
        JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
WHERE
    v.person_id = ?
  AND v.utfallkode != 'AVBRUTT'
  AND v.rettighetkode IN ( 'KLAG1', 'KLAG2' )
  AND v.MOD_DATO >= DATE '2020-01-01' -- ytelse: unngå å løpe gjennom veldig gamle vedtak, begrens string-til-dato konvertering
  AND vf.vedtakfaktakode = 'INNVF'
  -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.
  -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
  AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ? )
  -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger. Ekskluder disse.
  AND NOT ( vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) AND v.utfallkode IN ('JA', 'DELVIS' ) )

UNION ALL

SELECT
    v.sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    CAST(NULL AS DATE)                    AS fra_dato,
    TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
    v.rettighetkode
FROM
    vedtak v
        JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
WHERE
    v.person_id = ?
  AND rettighetkode = 'ANKE'
  AND utfallkode != 'AVBRUTT'
  AND v.MOD_DATO >= DATE '2020-01-01' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
  AND vf.vedtakfaktakode = 'KJREGDATO'
  AND ( vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= ADD_MONTHS(TRUNC(SYSDATE), -36) ) -- stor tidsbuffer her, da det kan ankes oppover i rettsvesenet.
  -- Dersom anken ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger.
  -- Vi bruker vedtakfaktakode=KJENNELSE fremfor vedtak.utfallkode=JA her, fordi vi ser uventet utfallkode for noen innvilgede anker i produksjon.
  AND NOT EXISTS(SELECT 1 from vedtakfakta vf_innvilget
                 WHERE vf_innvilget.vedtak_id = v.vedtak_id -- samme vedtaket
                   AND vf_innvilget.vedtakfaktakode = 'KJENNELSE'
                   AND vf_innvilget.vedtakverdi = 'JA' -- er innvilget (kan evt utvides med flere verdier)
                   AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= ADD_MONTHS(TRUNC(SYSDATE), -6) -- er minst 6 mnd siden
)

UNION ALL

SELECT
    v.sak_id,
    vedtakstatuskode,
    vedtaktypekode,
    CAST(NULL AS DATE)                    AS fra_dato,
    TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
    v.rettighetkode
FROM
    vedtak v
        JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
WHERE
    v.person_id = ?
  AND rettighetkode = 'TILBBET'
  AND utfallkode != 'AVBRUTT'
  AND v.MOD_DATO >= DATE '2021-01-01' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
  AND vf.vedtakfaktakode = 'INNVF'
  -- Vi regner tilbakebetalinger med null INNVF som åpne, ellers ikke.
  AND vf.vedtakverdi IS NULL -- det er ikke satt endelig dato for beslutning på vedtaket
-- SPM: finnes det en dato eller annet vi kan lese for å vite når tilbakebetalingen er fullført av personen?

UNION ALL

SELECT
    v.sak_id,
    su.vedtakstatuskode,
    CAST(NULL AS VARCHAR2(10))  AS vedtaktypekode,
    su.dato_fra AS fra_dato,
    su.dato_til AS til_dato,
    'SPESIAL' AS rettighetkode
FROM
    spesialutbetaling su
        JOIN vedtak v ON v.vedtak_id = su.vedtak_id -- for å få sak_id
WHERE
    su.person_id = ?
  -- Dersom utbetalingen ikke er datofestet, eller den har skjedd nylig, regner vi saken som åpen, ellers ikke.
  -- Vi bruker en tidsbuffer her i tilfelle det klages på spesialutbetalingen etter at den er utbetalt.
  AND (su.dato_utbetaling IS NULL OR su.dato_utbetaling >= ADD_MONTHS(TRUNC(SYSDATE), -3) )
-- MERK: ingen index i spesialutbetaling på dato_utbetaling eller andre dato-felt, så det går tregt

UNION ALL

SELECT
    v.sak_id,
    v.vedtakstatuskode,
    v.vedtakstatuskode,
    ssu.dato_periode_fra AS fra_dato,
    ssu.dato_periode_til AS til_dato,
    'SIM_SPESIAL' AS rettighetkode
FROM
    sim_utbetalingsgrunnlag ssu
        JOIN vedtak v ON v.vedtak_id = ssu.vedtak_id
WHERE
    ssu.person_id = ?
  -- MERK: ingen index i sim_utbetalingsgrunnlag på mod_dato eller andre datofelt, så blir tregt
  AND ssu.mod_dato >= ADD_MONTHS(TRUNC(SYSDATE), -3) -- ignorer gamle simuleringer som ikke ble noe av
-- SPM: er denne spørringen unødvendig, vil feks. et tilhørende vedtak for personen uansett
-- finnes i Vedtak (TILBBET) eller i Spesialutbetaling? {1: 4873545, 2: DATE '2021-04-03', 3: DATE '2020-06-20', 4: DATE '2021-04-03', 5: 4873545, 6: DATE '2021-04-03', 7: 4873545, 8: 4873545, 9: 4873545, 10: 4873545};
;

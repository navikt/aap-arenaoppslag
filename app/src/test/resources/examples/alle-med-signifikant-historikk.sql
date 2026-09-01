-- Eksempelspørring for å finne alle vedtak som kan være signifikant for om
-- personen kan tas inn i Kelvin eller ikke.

-- kreves i arena-q1:
-- alter session set current_schema = "ARENA_TILGANG_AAP";

-- antall personer totalt i arena-historikken
SELECT count(distinct person_id) as totalt_tall
from vedtak;

-- antall signifikante personer med denne utgaven av filter-spørringer
SELECT count(distinct person_id) as nyeste_tall
from (SELECT person_id,
             aar,
             lopenrvedtak,
             lopenrsak,
             vedtakstatuskode,
             vedtaktypekode,
             fra_dato,
             til_dato,
             rettighetkode,
             aktfasekode,
             utfallkode
      FROM vedtak v
      WHERE (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
        AND v.rettighetkode = 'AAP'
        AND v.MOD_DATO >= DATE '2019-12-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL))     -- filtrer ut ugyldiggjorte vedtak
        AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
        AND (
          ((vedtaktypekode IN ('O', 'E', 'G') OR (vedtaktypekode = 'S' and v.til_dato IS NOT NULL)) AND
           (til_dato IS NULL OR til_dato >= DATE '2024-06-17')) -- vanlig tidsbuffer
              OR
          (vedtaktypekode = 'S' AND til_dato IS NULL AND (fra_dato IS NULL OR fra_dato >= DATE '2023-09-04')
              -- En gammel sak kan ha endt med et stans-vedtak, men personen har en løpende ny sak.
              -- Ekskluder slike gamle stans-vedtak:
              AND NOT EXISTS(SELECT vedtak_id
                             FROM vedtak vv
                             WHERE vv.person_id = v.person_id -- for samme person
                               -- Samme begrensning som hovedspørringen:
                               AND vv.rettighetkode = 'AAP'
                               AND vv.vedtaktypekode IN ('O', 'E', 'G')
                               AND vv.vedtakstatuskode IN ('IVERK', 'AVSLU')
                               AND vv.utfallkode != 'AVBRUTT'
                               -- Et nyere vedtak erstatter denne stansen:
                               AND vv.vedtak_id > v.vedtak_id -- et nyere vedtak
                               AND (vv.fra_dato IS NOT NULL AND v.fra_dato IS NOT NULL AND
                                    vv.fra_dato > v.fra_dato) -- med nyere fra_dato
              )
              ) -- ekstra tidsbuffer for Stans, som bare har fra_dato
          )
        AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND (fra_dato IS NOT NULL AND fra_dato <= DATE '2024-06-17')) -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle
      UNION ALL
      SELECT person_id,
             aar,
             lopenrvedtak,
             lopenrsak,
             vedtakstatuskode,
             vedtaktypekode,
             fra_dato,
             til_dato,
             rettighetkode,
             aktfasekode,
             utfallkode
      FROM vedtak v
      WHERE (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
        AND v.rettighetkode = 'AA115'
        AND v.MOD_DATO >= DATE '2019-12-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL))     -- filtrer ut ugyldiggjorte vedtak
        AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
        AND (
          ((vedtaktypekode IN ('O', 'E', 'G') OR (vedtaktypekode = 'S' and v.til_dato IS NOT NULL)) AND
           (til_dato IS NULL OR til_dato >= DATE '2024-06-17')) -- vanlig tidsbuffer
              OR
          (vedtaktypekode = 'S' AND til_dato IS NULL AND (fra_dato IS NULL OR fra_dato >= DATE '2023-09-04')
              -- En gammel sak kan ha endt med et stans-vedtak, men personen har en løpende ny sak.
              -- Ekskluder slike gamle stans-vedtak:
              AND NOT EXISTS(SELECT vedtak_id
                             FROM vedtak vv
                             WHERE vv.person_id = v.person_id -- for samme person
                               -- Samme begrensning som hovedspørringen:
                               AND vv.rettighetkode = 'AA115'
                               AND vv.vedtaktypekode IN ('O', 'E', 'G')
                               AND vv.vedtakstatuskode IN ('IVERK', 'AVSLU')
                               AND vv.utfallkode != 'AVBRUTT'
                               -- Et nyere vedtak erstatter denne stansen:
                               AND vv.vedtak_id > v.vedtak_id -- et nyere vedtak
                               AND (vv.fra_dato IS NOT NULL AND v.fra_dato IS NOT NULL AND
                                    vv.fra_dato > v.fra_dato) -- med nyere fra_dato
              )
              ) -- ekstra tidsbuffer for Stans, som bare har fra_dato
          )
        AND NOT (utfallkode = 'NEI' AND til_dato IS NULL)                                     -- bruker fikk avslag
      UNION ALL
      -- INNVF er satt for alle klager. Den får alltid en dato-verdi når utfallet av klagen registreres.
      -- Dersom den er null, er klagen fortsatt under behandling.
      SELECT v.person_id,
             v.aar,
             v.lopenrvedtak,
             v.lopenrsak,
             vedtakstatuskode,
             vedtaktypekode,
             CAST(NULL AS DATE)                    AS fra_dato,
             TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') AS til_dato,
             v.rettighetkode,
             v.aktfasekode,
             v.utfallkode
      FROM vedtak v
               JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
      WHERE (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
        AND v.rettighetkode IN ('KLAG1', 'KLAG2')
        AND v.MOD_DATO >= DATE '2019-12-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        AND vf.vedtakfaktakode = 'INNVF'
        -- Vi regner klager med null INNVF som åpne. Klager med fersk INNVF-dato regnes også som åpne, pga. det tar tid før AAP-vedtakene registreres.
        -- Og at det kan komme en ny klage eller anke etter at klagen er behandlet og avslått. Anker sjekkes for seg selv.
        AND (vf.vedtakverdi IS NULL OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= DATE '2024-06-17')
        -- Dersom klagen ble innvilget for mer enn 6 mnd siden, regnes den som ikke relevant lenger. Ekskluder disse.
        AND NOT (vf.vedtakverdi IS NOT NULL AND TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') <= DATE '2025-06-15' AND
                 v.utfallkode IN ('JA', 'DELVIS'))
      UNION ALL
      SELECT v.person_id,
             v.aar,
             v.lopenrvedtak,
             v.lopenrsak,
             vedtakstatuskode,
             vedtaktypekode,
             CAST(NULL AS DATE) AS fra_dato,
             CAST(NULL AS DATE) AS til_dato,
             v.rettighetkode,
             v.aktfasekode,
             v.utfallkode
      FROM vedtak v
               JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
      WHERE (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
        AND rettighetkode = 'ANKE'
        AND v.MOD_DATO >= DATE '2019-12-15' -- ytelse: unngå å løpe gjennom veldig gamle vedtak

      ORDER BY aar DESC, lopenrsak DESC, lopenrvedtak DESC);
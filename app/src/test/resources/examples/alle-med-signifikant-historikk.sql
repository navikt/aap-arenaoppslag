-- Eksempelspørring for å finne alle personer med signifikant historikk i AAP-Arena

-- kreves i arena-q1:
-- alter session set current_schema = "ARENA_TILGANG_AAP";

-- antall personer med signifikant historikk i arena
select count(distinct person_id) as totalt_antall from vedtak;

-- antall personer med signifikant historikk med denne utgaven av filter-spørringer
select count(disticint person_id) as signifikant_antall
from (SELECT person_id,
             sak_id,
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
        AND v.MOD_DATO >= DATE '2020-09-21'                                                   -- ytelse: unngå å løpe gjennom veldig gamle vedtak
        AND NOT (fra_dato > til_dato AND (til_dato IS NOT NULL AND fra_dato IS NOT NULL))     -- filtrer ut ugyldiggjorte vedtak
        AND ((fra_dato IS NOT NULL OR til_dato IS NOT NULL) OR vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS',
                                                                                    'INNST')) -- filtrer ut etterregistrerte vedtak, men behold vedtak som er under behandling
        AND (
          ((vedtaktypekode IN ('O', 'E', 'G') OR (vedtaktypekode = 'S' and v.til_dato IS NOT NULL)) AND
           (til_dato IS NULL OR til_dato >= DATE '2025-03-24')) -- vanlig tidsbuffer
              OR
          (vedtaktypekode = 'S' AND til_dato IS NULL AND
           (fra_dato IS NULL OR fra_dato >= DATE '2024-06-10')) -- ekstra tidsbuffer for Stans, som bare har fra_dato
          )
        AND NOT (utfallkode = 'NEI' AND til_dato IS NULL AND
                 (fra_dato IS NOT NULL AND fra_dato <= DATE '2025-03-24'))                    -- utfallkode NEI vil ha åpen til_dato, så ekskluder disse når de er gamle
      UNION ALL
      SELECT person_id,
             sak_id,
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
      WHERE v.rettighetkode = 'AA115'
        AND v.utfallkode IS NULL -- ikke behandlet enda
        AND v.MOD_DATO >= DATE '2026-03-23' -- ikke utdatert

     );
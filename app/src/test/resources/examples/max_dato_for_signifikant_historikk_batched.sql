ALTER
SESSION
SET current_schema = "ARENA_TILGANG_AAP";


WITH personer_med_signifikant_historikk AS (SELECT v.person_id, v.vedtak_id, vedtaktypekode
                                            FROM vedtak v
                                            WHERE (v.utfallkode IS NULL OR v.utfallkode != 'AVBRUTT')
                                              AND v.aktfasekode IN ('AU', 'UA')
                                              AND v.rettighetkode = 'AAP'
                                              AND NOT (
                                                v.fra_dato > v.til_dato
                                                    AND (v.til_dato IS NOT NULL AND v.fra_dato IS NOT NULL)
                                                )
                                              AND (
                                                (v.fra_dato IS NOT NULL OR v.til_dato IS NOT NULL)
                                                    OR v.vedtakstatuskode IN ('OPPRE', 'MOTAT', 'REGIS', 'INNST')
                                                )
                                              AND (
                                                (
                                                    (
                                                        v.vedtaktypekode IN ('O', 'E', 'G')
                                                            OR (v.vedtaktypekode = 'S' AND v.til_dato IS NOT NULL)
                                                        )
                                                        AND (v.til_dato IS NULL OR v.til_dato >= DATE '2025-05-01')
                                                    )
                                                    OR (
                                                    v.vedtaktypekode = 'S'
                                                        AND v.til_dato IS NULL
                                                        AND (v.fra_dato IS NULL OR v.fra_dato >= DATE '2024-07-18')
                                                    )
                                                )
                                              AND NOT (
                                                v.utfallkode IS NOT NULL
                                                    AND v.utfallkode = 'NEI'
                                                    AND v.til_dato IS NULL
                                                    AND (v.fra_dato IS NOT NULL AND v.fra_dato <= DATE '2025-05-01')
                                                )
                                              AND NOT (v.utfallkode IS NULL AND v.mod_dato <= DATE '2025-10-30')
                                              AND v.mod_dato >= DATE '2022-11-03'

                                            UNION

                                            SELECT v.person_id, v.vedtak_id, vedtaktypekode
                                            FROM vedtak v
                                            WHERE v.rettighetkode = 'AA115'
                                              AND v.utfallkode IS NULL
                                              AND v.aktfasekode IN ('AU', 'UA')
                                              AND v.mod_dato >= DATE '2026-04-30'

                                            UNION

                                            SELECT v.person_id, v.vedtak_id, vedtaktypekode
                                            FROM vedtak v
                                                     JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
                                            WHERE v.rettighetkode IN ('KLAG1', 'KLAG2')
                                              AND v.aktfasekode IN ('AU', 'UA')
                                              AND (v.utfallkode IS NULL OR v.utfallkode NOT IN ('AVBRUTT', 'TRUKK'))
                                              AND vf.vedtakfaktakode = 'INNVF'
                                              AND (
                                                vf.vedtakverdi IS NULL
                                                    OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= DATE '2026-08-06'
                                                )
                                              AND NOT (v.utfallkode IS NULL AND v.mod_dato <= DATE '2025-10-30')
                                              AND v.mod_dato >= DATE '2023-11-02'

                                            UNION

                                            SELECT v.person_id, v.vedtak_id, vedtaktypekode
                                            FROM vedtak v
                                                     JOIN vedtakfakta vf ON vf.vedtak_id = v.vedtak_id
                                            WHERE v.rettighetkode = 'ANKE'
                                              AND v.aktfasekode IN ('AU', 'UA')
                                              AND (v.utfallkode IS NULL OR v.utfallkode NOT IN ('AVBRUTT', 'TRUKK'))
                                              AND vf.vedtakfaktakode = 'INNVF'
                                              AND (
                                                vf.vedtakverdi IS NULL
                                                    OR TO_DATE(vf.vedtakverdi, 'DD-MM-YYYY') >= DATE '2025-10-30'
                                                )
                                              AND NOT (v.utfallkode IS NULL AND v.mod_dato <= DATE '2025-10-30')
                                              AND v.mod_dato >= DATE '2023-11-02'),
     nyeste_vedtak AS (SELECT person_id, vedtak_id, vedtaktypekode
                       FROM (SELECT v.person_id,
                                    v.vedtak_id,
                                    v.vedtaktypekode,
                                    ROW_NUMBER() OVER (
                                        PARTITION BY v.person_id
                                        ORDER BY v.til_dato DESC NULLS FIRST, v.vedtak_id DESC
                                        ) AS rn
                             FROM vedtak v
                             WHERE v.vedtak_id IN (SELECT vedtak_id
                                                   FROM personer_med_signifikant_historikk
                                                   WHERE personer_med_signifikant_historikk.vedtaktypekode != 'S')
                               AND v.rettighetkode = 'AAP'
                               AND v.utfallkode = 'JA'
                               AND v.vedtakstatuskode IN ('IVERK', 'AVSLU')
                               AND (
                                 v.til_dato IS NOT NULL
                                     OR (
                                     v.vedtaktypekode = 'S'
                                         AND v.til_dato IS NULL
                                         AND NOT EXISTS (SELECT vv.vedtak_id
                                                         FROM vedtak vv
                                                         WHERE vv.person_id = v.person_id
                                                           AND vv.vedtak_id IN (SELECT vedtak_id
                                                                                FROM personer_med_signifikant_historikk
                                                                                WHERE personer_med_signifikant_historikk.vedtaktypekode != 'S')
                                                           AND vv.rettighetkode = 'AAP'
                                                           AND vv.utfallkode = 'JA'
                                                           AND vv.vedtakstatuskode IN ('IVERK', 'AVSLU')
                                                           AND vv.vedtak_id > v.vedtak_id
                                                           AND (
                                                             vv.fra_dato IS NOT NULL
                                                                 AND v.fra_dato IS NOT NULL
                                                                 AND vv.fra_dato > v.fra_dato
                                                             ))
                                         AND NOT (
                                         v.reg_user IS NOT NULL
                                             AND v.reg_user = 'GRENSESN'
                                             AND v.begrunnelse IS NOT NULL
                                             AND v.begrunnelse =
                                                 'Arbeidsavklaringspenger er stanset da til-dato for vedtaket er passert.'
                                         )
                                     )
                                 )
                               AND v.fra_dato IS NOT NULL
                               AND NOT (
                                 (v.fra_dato IS NOT NULL AND v.til_dato IS NOT NULL)
                                     AND v.fra_dato > v.til_dato
                                 ))
                       WHERE rn = 1)
SELECT DISTINCT p.person_id,
                nv.vedtak_id AS siste_vedtak_id,
                vmd.max_dato,
                vmd.max_unntak_dato
FROM personer_med_signifikant_historikk p
         LEFT JOIN nyeste_vedtak nv
                   ON nv.person_id = p.person_id
         LEFT JOIN v_vedtak_maxdato vmd
                   ON vmd.vedtak_id = nv.vedtak_id
WHERE nv.vedtak_id IS NOT NULL
  AND nv.vedtaktypekode != 'S'
    FETCH FIRST 10000 ROWS ONLY;

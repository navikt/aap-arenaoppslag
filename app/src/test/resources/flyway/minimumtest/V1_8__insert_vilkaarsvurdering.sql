-- Vilkårsvurderinger for vedtak_id=1234 (sak_id=1, person 123)
-- Bruker vilkårkoder som finnes i VILKAARTYPE fra V1_2__arena_konstanter.sql

Insert into VILKAARVURDERING (VILKAARVURDERING_ID, VEDTAKTYPEKODE, VILKAARKODE, VEDTAK_ID, REG_DATO, REG_USER,
                               MOD_DATO, MOD_USER, RETTIGHETKODE, AKTFASEKODE, VILKAARSTATUSKODE, VURDERT_AV,
                               BEGRUNNELSE)
values (1001, 'O', 'OPPHINST', 1234, DATE '2022-08-30', 'TEST',
        DATE '2022-08-30', 'TEST', 'AAP', 'IKKE', 'J', 'TEST01',
        'Vilkåret er oppfylt');

Insert into VILKAARVURDERING (VILKAARVURDERING_ID, VEDTAKTYPEKODE, VILKAARKODE, VEDTAK_ID, REG_DATO, REG_USER,
                               MOD_DATO, MOD_USER, RETTIGHETKODE, AKTFASEKODE, VILKAARSTATUSKODE, VURDERT_AV,
                               BEGRUNNELSE)
values (1002, 'O', 'PAASTAND', 1234, DATE '2022-08-30', 'TEST',
        DATE '2022-08-30', 'TEST', 'AAP', 'IKKE', 'N', 'TEST01',
        null);

-- Testdata for oppholdskrav-endepunktet i migrering (vilkårskode AOINORGE3).
-- Gjenbruker sakene fra refusjonskrav: 2023-505 har AOINORGE3 på aap-vedtaket, 2023-506 har ikke.
insert into VILKAARVURDERING (VILKAARVURDERING_ID, VEDTAKTYPEKODE, VILKAARKODE, VEDTAK_ID, REG_DATO, REG_USER,
                              MOD_DATO, MOD_USER, RETTIGHETKODE, AKTFASEKODE, VILKAARSTATUSKODE, VURDERT_AV,
                              BEGRUNNELSE)
values (910511, 'O', 'AOINORGE3', 91051, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AAP', 'UA', 'J',
        'TEST01', null),
       -- Annet vilkår på samme vedtak skal ignoreres
       (910512, 'O', 'SYKSKADLYT', 91051, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AAP', 'UA', 'J',
        'TEST01', null);

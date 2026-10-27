-- Testdata for forutgående medlemskap-endepunktet i migrering (vilkårskode MEDLSKAP5).
-- Gjenbruker sakene fra refusjonskrav: 2023-505 har MEDLSKAP5 på aap-vedtaket, 2023-506 har ikke.
insert into VILKAARVURDERING (VILKAARVURDERING_ID, VEDTAKTYPEKODE, VILKAARKODE, VEDTAK_ID, REG_DATO, REG_USER,
                              MOD_DATO, MOD_USER, RETTIGHETKODE, AKTFASEKODE, VILKAARSTATUSKODE, VURDERT_AV,
                              BEGRUNNELSE)
values (910513, 'O', 'MEDLSKAP5', 91051, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AAP', 'UA', 'J',
        'TEST01', 'Medlem i folketrygden siste fem år'),
       -- Eldre kode for samme vilkår skal ignoreres
       (910514, 'O', 'MEDLSKAP1', 91051, CURRENT_DATE, 'TEST', CURRENT_DATE, 'TEST', 'AAP', 'UA', 'N',
        'TEST01', null);

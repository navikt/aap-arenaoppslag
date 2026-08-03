-- Oppgaver for person 1: to med frist (ulik dato) og en uten frist,
-- slik at sortering på DUEDATE DESC NULLS LAST kan verifiseres.
insert into V_OPPGAVE (PERSON_ID, FODSELSNR, TASK_ID, DESCRIPTION, CASECONTEXT, DISPLAYNAME, DUEDATE, ARBEIDSBENK,
                       OPPGAVE_ENHET, NAV_ENHET, NOTE)
values (1, '123', 1001, 'Vurder rett til AAP', 'AA', 'Vurder rettighet', DATE '2024-05-01', 'Min benk',
        '0826', '4402', 'Notat om oppgaven'),
       (1, '123', 1002, 'Behandle meldekort', 'AA', 'Meldekort', DATE '2024-09-15', 'Enhetens benk',
        '0826', '4402', null),
       (1, '123', 1003, 'Oppgave uten frist', null, null, null, null,
        null, null, null);

-- Oppgave for annen person, brukes til å verifisere filtrering på PERSON_ID
insert into V_OPPGAVE (PERSON_ID, FODSELSNR, TASK_ID, DESCRIPTION, CASECONTEXT, DISPLAYNAME, DUEDATE, ARBEIDSBENK,
                       OPPGAVE_ENHET, NAV_ENHET, NOTE)
values (2, '321', 1004, 'Oppgave for annen person', 'AA', 'Annen person', DATE '2024-01-01', 'Min benk',
        '0826', '4402', null);


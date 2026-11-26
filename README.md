# AAP arenaoppslag

Applikasjon som skal brukes til å gjøre SQL-oppslag mot Arena via dedikerte views for AAP. 
Kjører i FSS og henter Arena-tilkobling fra Vault.
Skal kun kalles fra våre egne interne apper, ikke lag åpning for apper utenfor vårt namespace.

# Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`.
Det er ikke lagt opp til at denne skal kjøre standalone, siden den er avhengig av Arena.

## Lokal TestApp med fast tokenport

`TestApp` bruker port `8087` for API-et. Tokenserveren (Texas) velger en ledig port ved hver oppstart.
For å bruke en fast tokenport, sett `-Darenaoppslag.test.texas.port=60933` i **VM options** i IntelliJ-konfigurasjonen
`TestApp Arenaoppslag som virker`. Sett `ARENAOPPSLAG_MOCK_OBO_TOKEN_URL=http://127.0.0.1:60933/token`
i visningsklientens `.env.local-backend`, og start begge applikasjonene på nytt.
Vanlige tester bruker fortsatt en ledig port. Hvis den valgte porten er opptatt, feiler oppstarten.

## Tilgangssjekk

Arenaoppslag kaller på AAP-tilgang for å sjekke om innlogget bruker har tilgang til å se AAP-informasjon i Arena for en gitt person eller en gitt sak. Dette gjøres per nå kun for kall fra Visningsklienten, men kan utvides til andre klienter senere. Trolig ikke mulig for kall fra aap-api (eksterne), som ikke gjøres i kontekst av en personlig bruker. 

# Tabeller i Arena

Tabellene i Arena er beskrevet av 
[Arena - Datamodell](https://confluence.adeo.no/spaces/ARENA/pages/122716553/Arena+-+Datamodell)

Vi bruker views av disse tabellene med filter for AAP, definert her.
Disse har samme kolonner som tabellene.
[Arena - løsningsbeskrivelse](https://confluence.adeo.no/spaces/TEAMARENA/pages/553617512/ARENA-8716+03+-+L%C3%B8sningsbeskrivelse#ARENA871603L%C3%B8sningsbeskrivelse-Arbeidsavklaringspenger)

Se også [tabelldefinisjoner med kommentarer som beskriver hvert felt](https://github.com/navikt/aap-arenaoppslag/blob/main/app/src/test/resources/flyway/common/V1_1__arena_schema.sql). 

## Databaseskjema for tester
Databasen for tester er basert på Arena sitt databaseskjema. 
Metoden for å ta inn tabeller og data fra Arena er slik: 
1. Vi får DDL av de tabellene det gjelder, og legger den til i 
[arena_aap_oracle_ddl_export.sql](app/src/test/resources/arena_aap_oracle_ddl_export.sql)
2. Vi ber Copilot om å oversette fra Oracle-19 DDL til H2 med Oracle-dialekt, inkludert bruk av DATE fremfor to_date for å unngå locale-variasjoner maskin til maskin. 
3. De nye tabellene legges inn i [V1_1__arena_schema.sql](app/src/test/resources/flyway/common/V1_1__arena_schema.sql)
4. Dersom de nye tabellene har type-data (enum/definisjoner/statiske data) legges det inn i [V1_3__arena_data.sql](app/src/test/resources/flyway/common/V1_3__arena_data.sql)

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.

Dette repoet er satt opp for AI-agenter med tydelige instruksjoner:

- `AGENTS.md` er hovedkilden for regler og prioritering av instruksjoner.
- `.github/copilot-instructions.md` skal holdes minimal og bare peke til `AGENTS.md`.
- `docs/ai/agent-guidelines-template.md` brukes som template om man ønsker tillegg i enkelte undermapper (ikke duplisering av globale regler).

Tips for nye oppgaver til AI:

- Beskriv hva som skal endres (konkret scope).
- Nevn om atferd skal bevares eller endres.
- Be om testoppdateringer ved logikkendringer.


# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub. 

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-aap-åpen.

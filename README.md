# AAP arenaoppslag

Applikasjon som skal brukes til å gjøre SQL-oppslag mot Arena via dedikerte views for AAP. 
Kjører i FSS og henter Arena-tilkobling fra Vault.
Skal kun kalles fra våre egne interne apper, ikke lag åpning for apper utenfor vårt namespace.

# Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`.
Det er ikke lagt opp til at denne skal kjøre standalone, siden den er avhengig av Arena.

# Tabeller i Arena

Tabellene i Arena er beskrevet av 
[Arena - Datamodell](https://confluence.adeo.no/spaces/ARENA/pages/122716553/Arena+-+Datamodell)

Vi bruker views av disse tabellene med filter for AAP, definert her.
Disse har samme kolonner som tabellene.
[Arena - løsningsbeskrivelse](https://confluence.adeo.no/spaces/TEAMARENA/pages/553617512/ARENA-8716+03+-+L%C3%B8sningsbeskrivelse#ARENA871603L%C3%B8sningsbeskrivelse-Arbeidsavklaringspenger)

Se også [tabelldefinisjoner med kommentarer som beskriver hvert felt](https://github.com/navikt/aap-arenaoppslag/blob/main/app/src/test/resources/flyway/common/V1_1__arena_schema.sql). 

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub. 

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-aap-åpen.
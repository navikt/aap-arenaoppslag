# AAP arenaoppslag

Applikasjon som skal brukes til å gjøre SQL-oppslag mot Arena via dedikerte views. Kjører i FSS og henter Arena-tilkobling fra Vault.
Skal kun kalles fra våre egne interne apper, ikke lag åpning for apper utenfor vårt namespace.

# Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`
Det er ikke lagt opp til at denne skal kjøre standalone, siden den er avhengig av Arena.
All logikk ligger i testene.

# Tabeller i Arena

Tabellene i Arena er beskrevet her:
[https://confluence.adeo.no/spaces/ARENA/pages/122716553/Arena+-+Datamodell](Arena - Datamodell)

Vi bruker views av disse tabellene for AAP, definert her.
Disse har samme felt som tabellene.
[https://confluence.adeo.no/spaces/TEAMARENA/pages/553617512/ARENA-8716+03+-+L%C3%B8sningsbeskrivelse#ARENA871603L%C3%B8sningsbeskrivelse-Arbeidsavklaringspenger](Arena - løsningsbeskrivelse)

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #po-aap-team-aap.
package arenaoppslag

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds

data class AppConfig(
    val proxyUrl: String = getEnvVar("HTTP_PROXY"),
    val enableProxy: Boolean = true,
    val database: DbConfig = DbConfig(),
    val azure: AzureConfig = AzureConfig(),
) {
    companion object {
        // Vi endrer ktor sin default-verdi som er "antall CPUer" synlige for JVM-en, som normalt er antall tilgjengelige kjener på container-hosten.
        // Dette kan gi et for høyt antall tråder i forhold. På den andre siden har vi en del venting på IO (db, http-auth).
        // Sett den til en balansert verdi:
        const val ktorParallellitet: Int = 4 // defaulter ellers til 2 pga "-XX:ActiveProcessorCount=2" i Dockerfile

        // Matcher terminationGracePeriodSeconds for podden i Kubernetes-manifestet ("nais.yaml")
        private val kubernetesTimeout = 30.seconds

        // Tid før ktor avslutter uansett. Må være litt mindre enn `kubernetesTimeout`.
        val shutdownTimeout = kubernetesTimeout - 2.seconds

        // Tid appen får til å fullføre påbegynte requests, jobber etc. Må være mindre enn `endeligShutdownTimeout`.
        val shutdownGracePeriod = shutdownTimeout - 3.seconds

        // Tid appen får til å avslutte Motor, Kafka, etc
        val stansArbeidTimeout = shutdownGracePeriod - 1.seconds
    }
}

data class DbConfig(
    val url: String = runCatching { Path(getEnvVar("DB_JDBC_URL_PATH")).readText() }.getOrElse {
        logger.warn( "Could not read DB_JDBC_URL_PATH " )
        "localhost"
    },
    val username: String = runCatching { Path(getEnvVar("DB_USERNAME_PATH")).readText() }.getOrElse {
        logger.warn("Could not read DB_USERNAME_PATH " )
        "username"
    },
    val password: String = runCatching { Path(getEnvVar("DB_PASSWORD_PATH")).readText() }.getOrElse {
        logger.warn("Could not read DB_PASSWORD_PATH ")
        "password"
    },
    val driver: String = "oracle.jdbc.OracleDriver"
)

data class AzureConfig(
    val jwksUri: String = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
    val issuer: String = getEnvVar("AZURE_OPENID_CONFIG_ISSUER"),
    val clientId: String = getEnvVar("AZURE_APP_CLIENT_ID")
)

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

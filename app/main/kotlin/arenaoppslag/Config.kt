package arenaoppslag

import kotlin.io.path.Path
import kotlin.io.path.readText

data class Config(
    val proxyUrl: String = getEnvVar("HTTP_PROXY"),
    val enableProxy: Boolean = true,
    val database: DbConfig = DbConfig(),
    val azure: AzureConfig = AzureConfig()
)

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

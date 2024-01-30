package arenaoppslag

data class Config(
    val proxyUrl: String = getEnvVar("HTTP_PROXY"),
    val database: DbConfig = DbConfig(),
    val azure: AzureConfig = AzureConfig()
)

data class DbConfig(
    val url: String = getEnvVar("DB_JDBC_URL"),
    val username: String = getEnvVar("DB_USERNAME"),
    val password: String = getEnvVar("DB_PASSWORD"),
    val driver: String = "oracle.jdbc.OracleDriver"
)

data class AzureConfig(
    val jwksUri: String = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
    val issuer: String = getEnvVar("AZURE_OPENID_CONFIG_ISSUER"),
    val clientId: String = getEnvVar("AZURE_APP_CLIENT_ID")
)

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

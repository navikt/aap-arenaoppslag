package arenaoppslag

data class Config(
    val proxyUrl: String,
    val database: DbConfig,
    val azure: AzureConfig
)

data class DbConfig(
    val url: String,
    val username: String,
    val password: String
)

data class AzureConfig(
    val jwksUri: String,
    val issuer: String,
    val clientId: String
)

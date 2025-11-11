package arenaoppslag

import arenaoppslag.util.Fakes
import arenaoppslag.util.port
import java.net.URI

internal object TestConfig {
    internal val oracleH2 = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:request_no;MODE=Oracle",
        driver = "org.h2.Driver"
    )

    fun default(fakes: Fakes): AppConfig {
        return AppConfig(
            proxyUrl = "http://localhost",
            enableProxy = false,
            database = oracleH2,
            azure = AzureConfig(
                jwksUri = URI.create("http://localhost:${fakes.azure.port()}/jwks").toString(),
                issuer = "azure",
                clientId = "arenaoppslag"
            )
        )
    }
}
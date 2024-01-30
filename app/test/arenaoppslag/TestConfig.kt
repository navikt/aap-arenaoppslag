package arenaoppslag

internal object TestConfig {
    internal val postgres = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:request_no;MODE=Oracle",
        driver = "org.h2.Driver"
    )
}
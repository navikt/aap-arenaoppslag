package no.nav.aap.arenaoppslag.graphql

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)

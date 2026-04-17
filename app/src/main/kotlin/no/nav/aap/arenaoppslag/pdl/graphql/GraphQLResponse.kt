package no.nav.aap.arenaoppslag.pdl.graphql

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)

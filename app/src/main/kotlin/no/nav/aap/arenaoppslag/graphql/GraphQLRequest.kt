package no.nav.aap.arenaoppslag.graphql

data class GraphQLRequest<Variables>(
    val query: String,
    val variables: Variables,
)

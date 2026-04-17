package no.nav.aap.arenaoppslag.pdl.graphql

data class GraphQLRequest<Variables>(
    val query: String,
    val variables: Variables,
)

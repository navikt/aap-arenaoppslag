package no.nav.aap.arenaoppslag.pdl.graphql

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString

/**
 * Kopiet fra Api-inter repo https://github.com/navikt/aap-api-intern/tree/main/app/src/main/kotlin/no/nav/aap/api/util/graphql
 */

class GraphQLResponseHandler : RestResponseHandler<InputStream> {
    private val defaultResponseHandler = DefaultResponseHandler()

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R,
    ): R? {
        val håndtertResponse = defaultResponseHandler.håndter(request, response, mapper)

        if (håndtertResponse != null && håndtertResponse is GraphQLResponse<*>) {
            if (håndtertResponse.errors?.isNotEmpty() == true) {
                val feilmeldinger = håndtertResponse.errors.joinToString(transform = GraphQLError::message)
                throw GraphQLQueryException(
                    "Feil $feilmeldinger ved GraphQL oppslag mot ${request.uri()}",
                    håndtertResponse.errors.first().extensions.code ?: "Ukjent"
                )
            }
        }

        return håndtertResponse
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> = defaultResponseHandler.bodyHandler()
}

class GraphQLQueryException(
    msg: String,
    val code: String
) : RuntimeException(msg)

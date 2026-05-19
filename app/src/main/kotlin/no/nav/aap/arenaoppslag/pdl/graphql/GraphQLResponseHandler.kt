package no.nav.aap.arenaoppslag.pdl.graphql

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import no.nav.aap.arenaoppslag.pdl.PdlIdenter
import no.nav.aap.arenaoppslag.pdl.PdlIdenterData
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

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
                val feilmelding = håndtertResponse.errors.first()

                when (val errorCode = feilmelding.extensions.code) {
                    "bad_request" -> throw UgyldigForespørselException("Bad request fra PDL med feilmelding: '${feilmelding.message}'")
                    "not_found" -> GraphQLResponse(data = PdlIdenterData(PdlIdenter(emptyList())), errors = null)
                    else -> throw InternfeilException("Ukjent feil fra PDL (${errorCode}): ${feilmelding.message}")
                }
            }
        }

        return håndtertResponse
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> = defaultResponseHandler.bodyHandler()
}

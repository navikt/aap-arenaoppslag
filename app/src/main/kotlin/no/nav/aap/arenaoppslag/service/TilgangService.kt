package no.nav.aap.arenaoppslag.service

import no.nav.aap.arenaoppslag.modeller.SakIdentifikator
import no.nav.aap.arenaoppslag.tilgangsmaskin.TilgangmaskinGateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class TilgangService(private val sakService: SakService, private val tilgangmaskinGateway: TilgangmaskinGateway) {
    fun harTilgangTilPerson(brukerIdent: String, token: OidcToken): Authorized<String> {
        return when(tilgangmaskinGateway.harTilgangTilPerson(brukerIdent, token)) {
            true -> Authorized.HarTilgang(brukerIdent)
            false -> Authorized.IkkeTilgang
        }
    }

    fun harTilgangTilSak(sakIdentifikator: SakIdentifikator, token: OidcToken): Authorized<String> {
        val person = sakService.hentPersonForSak(sakIdentifikator) ?: return Authorized.IkkeFunnet
        return harTilgangTilPerson(person.fodselsnummer, token)
    }
}

sealed class Authorized<out T> {
    data class HarTilgang<T>(val item: T): Authorized<T>()
    data object IkkeTilgang: Authorized<Nothing>()
    data object IkkeFunnet: Authorized<Nothing>()

    fun getOrNull(): T? {
        if (this is HarTilgang) {
            return item
        }
        return null
    }

    fun <G> map(block: (T) -> G): Authorized<G> = when (this) {
        is HarTilgang -> HarTilgang(block(item))
        IkkeTilgang -> IkkeTilgang
        IkkeFunnet -> IkkeFunnet
    }

    fun <G : Any> mapNotNull(block: (T) -> G?): Authorized<G> = when (this) {
        is HarTilgang -> when (val result = block(item)) {
            null -> IkkeFunnet
            else -> HarTilgang(result)
        }
        IkkeTilgang -> IkkeTilgang
        IkkeFunnet -> IkkeFunnet
    }

    fun onError(block: () -> Unit): Authorized<T> {
        if (this !is HarTilgang) {
            block()
        }
        return this
    }

    suspend inline fun fold(suksess: suspend (T) -> Unit, ikkeFunnet: suspend () -> Unit, ikkeTilgang: suspend () -> Unit): Authorized<T> {
        when (this) {
            is HarTilgang -> suksess(item)
            IkkeTilgang -> ikkeTilgang()
            IkkeFunnet -> ikkeFunnet()
        }
        return this
    }
}
package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortAnmerkning
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.MeldekortReduksjon
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.SakId
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class MeldekortRepository(
    private val dataSource: DataSource,
    // Oracle har en hard grense på 1000 elementer i IN-lister. Vi chunker for å holde oss under denne grensen.
    private val chunkStørrelse: Int = 999,
) {

    fun hentForSak(sakId: SakId): MeldekortForSak = dataSource.connection.use { con ->
        MeldekortForSak(
            posteringer = selectPosteringer(sakId, con),
            meldekort = selectMeldekort(sakId, con),
        )
    }

    private fun selectPosteringer(sakId: SakId, connection: Connection): List<MeldekortPostering> =
        connection.createParameterizedQuery(posteringerForSakSql).use { preparedStatement ->
            preparedStatement.setInt(1, sakId.id)
            preparedStatement.executeQuery().map { row ->
                // wasNull() må sjekkes rett etter getLong, før vi leser andre kolonner.
                val meldekortId = row.getLong("meldekort_id").let { if (row.wasNull()) null else it }
                val kildeObjektId = row.getLong("objekt_id_kilde").let { if (row.wasNull()) null else it }
                val kildeAlias = row.getString("tabellnavnalias_kilde")
                MeldekortPostering(
                    vedtakId = row.getInt("vedtak_id"),
                    personId = row.getInt("person_id"),
                    meldekortId = meldekortId,
                    periode = Periode(
                        fraOgMedDato = row.getDate("dato_periode_fra").toLocalDate(),
                        tilOgMedDato = row.getDate("dato_periode_til").toLocalDate(),
                    ),
                    belop = row.getInt("belop"),
                    dagsatsMedBarnetillegg = row.getString("dagsats_med_barnetillegg")?.toIntOrNull(),
                    dagsats = row.getString("dagsats")?.toIntOrNull(),
                    dagsatsForSamordning = row.getString("dagsats_for_samordning")?.toIntOrNull(),
                    insGrad = row.getString("ins_grad")?.toIntOrNull(),
                    kilde = PosteringKilde.fraKode(kildeAlias),
                    kildeAlias = kildeAlias,
                    kildeObjektId = kildeObjektId,
                )
            }
        }

    private fun selectMeldekort(sakId: SakId, connection: Connection): List<Meldekort> {
        val metadata = connection.createParameterizedQuery(meldekortForSakSql).use { preparedStatement ->
            preparedStatement.setInt(1, sakId.id)
            // Åpne vedtak (til_dato = NULL) avgrenses av dagens dato. Datoen sendes inn som parameter
            // i stedet for SYSDATE slik at spørringen oppfører seg likt i Oracle og H2.
            preparedStatement.setDate(2, Date.valueOf(LocalDate.now()))
            preparedStatement.setInt(3, sakId.id)
            preparedStatement.setInt(4, sakId.id)
            preparedStatement.executeQuery().map { row -> mapMeldekortMetadata(row) }
        }
        if (metadata.isEmpty()) return emptyList()

        val meldekortIder = metadata.map { it.meldekortId }
        val dagerPerMeldekort = selectMeldekortdager(metadata.associateBy { it.meldekortId }, connection)
        val anmerkningerPerMeldekort = selectAnmerkninger(meldekortIder, connection)

        return metadata.map { meta ->
            val anmerkninger = anmerkningerPerMeldekort[meta.meldekortId].orEmpty()
            Meldekort(
                meldekortId = meta.meldekortId,
                personId = meta.personId,
                periode = Periode(meta.datoFra, meta.datoTil),
                ukenrUke1 = meta.ukenrUke1,
                ukenrUke2 = meta.ukenrUke2,
                meldedato = meta.meldedato,
                meldeform = meta.meldeform,
                fortsattRegistrertArbeidssoker = meta.fortsattArbeidssoker,
                kommentar = meta.kommentar,
                dager = dagerPerMeldekort[meta.meldekortId].orEmpty(),
                reduksjon = tilReduksjon(anmerkninger),
                anmerkninger = anmerkninger,
                beregningStatusKode = meta.beregningStatusKode,
            )
        }
    }

    private fun selectMeldekortdager(
        metadataPerId: Map<Long, MeldekortMetadata>,
        connection: Connection,
    ): Map<Long, List<MeldekortDag>> {
        if (metadataPerId.isEmpty()) return emptyMap()
        return metadataPerId.keys.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = meldekortdagerSql(chunk)
            connection.createParameterizedQuery(sql).use { preparedStatement ->
                preparedStatement.executeQuery().map { row ->
                    val meldekortId = row.getLong("meldekort_id")
                    val ukenr = row.getInt("ukenr")
                    val dagnr = row.getInt("dagnr")
                    val meta = metadataPerId.getValue(meldekortId)
                    meldekortId to MeldekortDag(
                        ukenr = ukenr,
                        dagnr = dagnr,
                        dato = meta.datoFra.plusDays((ukeforskyvningIDager(ukenr, meta) + (dagnr - 1)).toLong()),
                        timerArbeidet = row.getDouble("timer_arbeidet"),
                        annetFravaer = row.getString("status_annetfravaer") == "J",
                    )
                }
            }
        }.groupBy({ it.first }, { it.second })
    }

    // Ukenumrene er kalenderuker, så subtraksjon av ukenummer feiler over årsskiftet
    // (uke 52 etterfulgt av uke 1 ville gitt en negativ forskyvning på nesten et år).
    // Vi utleder derfor forskyvningen av om raden hører til første eller andre uke i meldekortperioden.
    private fun ukeforskyvningIDager(ukenr: Int, meta: MeldekortMetadata): Int = when (ukenr) {
        meta.ukenrUke1 -> 0
        meta.ukenrUke2 -> DAGER_PER_UKE
        // Ukjente ukenummer behandles som første uke — meldekortperioden er alltid nøyaktig to uker.
        else -> 0
    }

    private fun selectAnmerkninger(
        meldekortIder: List<Long>,
        connection: Connection,
    ): Map<Long, List<MeldekortAnmerkning>> {
        if (meldekortIder.isEmpty()) return emptyMap()
        return meldekortIder.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = anmerkningerForMeldekortlisteSql(chunk)
            connection.createParameterizedQuery(sql).use { preparedStatement ->
                preparedStatement.executeQuery().map { row ->
                    row.getLong("objekt_id") to MeldekortAnmerkning(
                        kode = row.getString("anmerkningkode"),
                        navn = row.getString("anmerkningnavn"),
                        beskrivelse = row.getString("beskrivelse"),
                        verdi = row.getIntOrNull("verdi"),
                        verdi2 = row.getIntOrNull("verdi2"),
                    )
                }
            }
        }.groupBy({ it.first }, { it.second })
    }

    // Reduksjonstallene er summen av verdiene på de tre anmerkningkodene som påvirker utbetalingen.
    private fun tilReduksjon(anmerkninger: List<MeldekortAnmerkning>) = MeldekortReduksjon(
        dagerForSent = summerVerdi(anmerkninger, KODE_FOR_SENT),
        fravar = summerVerdi(anmerkninger, KODE_ANNET_FRAVAER).toFloat(),
        sykedager = summerVerdi(anmerkninger, KODE_SYKEDAGER).toFloat(),
    )

    private fun summerVerdi(anmerkninger: List<MeldekortAnmerkning>, kode: String): Int =
        anmerkninger.filter { it.kode == kode }.sumOf { it.verdi ?: 0 }

    private fun mapMeldekortMetadata(row: ResultSet) = MeldekortMetadata(
        meldekortId = row.getLong("meldekort_id"),
        personId = row.getInt("person_id"),
        datoFra = row.getDate("dato_fra").toLocalDate(),
        datoTil = row.getDate("dato_til").toLocalDate(),
        ukenrUke1 = row.getInt("ukenr_uke1"),
        ukenrUke2 = row.getInt("ukenr_uke2"),
        meldedato = row.getDate("dato_innkommet")?.toLocalDate(),
        meldeform = row.getString("mkskortkode"),
        fortsattArbeidssoker = tilBoolean(row.getString("status_fortsatt_arbeidsoker")),
        kommentar = row.getString("kommentar"),
        beregningStatusKode = row.getString("beregningstatuskode"),
    )

    private fun tilBoolean(verdi: String?): Boolean? = when (verdi) {
        "J" -> true
        "N" -> false
        else -> null
    }

    private data class MeldekortMetadata(
        val meldekortId: Long,
        val personId: Int,
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val ukenrUke1: Int,
        val ukenrUke2: Int,
        val meldedato: LocalDate?,
        val meldeform: String?,
        val fortsattArbeidssoker: Boolean?,
        val kommentar: String?,
        val beregningStatusKode: String?,
    )

    @Language("OracleSql")
    private val posteringerForSakSql = """
        SELECT p.vedtak_id, p.person_id, p.meldekort_id, p.dato_periode_fra, p.dato_periode_til, p.belop,
               p.tabellnavnalias_kilde, p.objekt_id_kilde,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGSMBT') AS dagsats_med_barnetillegg,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGS') AS dagsats,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'DAGSFSAM') AS dagsats_for_samordning,
               (SELECT MAX(vf.vedtakverdi)
                  FROM vedtakfakta vf
                 WHERE vf.vedtak_id = p.vedtak_id
                   AND vf.vedtakfaktakode = 'INSGRAD') AS ins_grad
          FROM postering p
          JOIN vedtak v ON v.vedtak_id = p.vedtak_id
         WHERE v.sak_id = ?
         ORDER BY p.dato_periode_fra, p.postering_id
    """.trimIndent()

    // Meldekort uten utbetaling har ingen postering, og ville falt ut om vi bare joinet mot POSTERING.
    // Andre del av unionen henter derfor meldekortene til personen innenfor sakens vedtaksvindu.
    // 'DP' er dagpenge-meldekort og hører ikke til en AAP-sak, og meldekort som allerede er postert
    // på en annen sak filtreres bort slik at et meldekort kun vises på én sak.
    @Language("OracleSql")
    private val meldekortForSakSql = """
        SELECT m.meldekort_id, m.person_id, m.dato_innkommet, m.mkskortkode,
               m.status_fortsatt_arbeidsoker, m.kommentar, m.beregningstatuskode,
               mkp.dato_fra, mkp.dato_til, mkp.ukenr_uke1, mkp.ukenr_uke2
          FROM meldekort m
          JOIN meldekortperiode mkp ON mkp.aar = m.aar AND mkp.periodekode = m.periodekode
         WHERE m.meldekort_id IN (SELECT p.meldekort_id
                                    FROM postering p
                                    JOIN vedtak v ON v.vedtak_id = p.vedtak_id
                                   WHERE v.sak_id = ?)
        UNION
        SELECT m.meldekort_id, m.person_id, m.dato_innkommet, m.mkskortkode,
               m.status_fortsatt_arbeidsoker, m.kommentar, m.beregningstatuskode,
               mkp.dato_fra, mkp.dato_til, mkp.ukenr_uke1, mkp.ukenr_uke2
          FROM meldekort m
          JOIN meldekortperiode mkp ON mkp.aar = m.aar AND mkp.periodekode = m.periodekode
          JOIN (SELECT v.person_id,
                       MIN(v.fra_dato) AS fra_dato,
                       MAX(COALESCE(v.til_dato, ?)) AS til_dato
                  FROM vedtak v
                 WHERE v.sak_id = ?
                   AND v.fra_dato IS NOT NULL
                 GROUP BY v.person_id) saksvindu ON saksvindu.person_id = m.person_id
         WHERE mkp.dato_til >= saksvindu.fra_dato
           AND mkp.dato_fra <= saksvindu.til_dato
           AND (m.meldekortkode IS NULL OR m.meldekortkode <> 'DP')
           AND NOT EXISTS (SELECT 1
                             FROM postering p2
                             JOIN vedtak v2 ON v2.vedtak_id = p2.vedtak_id
                            WHERE p2.meldekort_id = m.meldekort_id
                              AND v2.sak_id <> ?)
         ORDER BY dato_fra, meldekort_id
    """.trimIndent()

    // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
    private fun meldekortdagerSql(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT meldekort_id, ukenr, dagnr, timer_arbeidet, status_annetfravaer
              FROM meldekortdag
             WHERE meldekort_id IN ($idListe)
             ORDER BY meldekort_id, ukenr, dagnr
        """.trimIndent()
    }

    // Oracle støtter ikke listeparametere i PreparedStatement, så meldekort-IDer interpoleres direkte.
    private fun anmerkningerForMeldekortlisteSql(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT a.objekt_id, a.anmerkningkode, a.verdi, a.verdi2,
                   at.anmerkningnavn, at.beskrivelse
              FROM anmerkning a
              LEFT JOIN anmerkningtype at ON at.anmerkningkode = a.anmerkningkode
             WHERE a.tabellnavnalias = 'MKORT'
               AND a.objekt_id IN ($idListe)
             ORDER BY a.objekt_id, a.anmerkning_id
        """.trimIndent()
    }

    private companion object {
        private const val DAGER_PER_UKE = 7

        // Anmerkningkoder som reduserer utbetalingen: for sent levert meldekort, annet fravær og sykdom.
        private const val KODE_FOR_SENT = "SENN"
        private const val KODE_ANNET_FRAVAER = "FXNN"
        private const val KODE_SYKEDAGER = "FSNN"
    }
}




package no.nav.aap.arenaoppslag.database

import no.nav.aap.arenaoppslag.modeller.Meldekort
import no.nav.aap.arenaoppslag.modeller.MeldekortAnmerkning
import no.nav.aap.arenaoppslag.modeller.MeldekortDag
import no.nav.aap.arenaoppslag.modeller.MeldekortForSak
import no.nav.aap.arenaoppslag.modeller.MeldekortPostering
import no.nav.aap.arenaoppslag.modeller.Periode
import no.nav.aap.arenaoppslag.modeller.PosteringKilde
import no.nav.aap.arenaoppslag.modeller.SakId
import no.nav.aap.arenaoppslag.modeller.Spesialutbetaling
import no.nav.aap.arenaoppslag.modeller.reduksjonFra
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
            // Saken filtrerer både vedtaksfaktaene i WITH-blokken og posteringene i hovedspørringen.
            preparedStatement.setInt(1, sakId.id)
            preparedStatement.setInt(2, sakId.id)
            preparedStatement.executeQuery().map { row ->
                // wasNull() må sjekkes rett etter getLong, før vi leser andre kolonner.
                val meldekortId = row.getLong("meldekort_id").let { if (row.wasNull()) null else it }
                val kildeObjektId = row.getLong("objekt_id_kilde").let { if (row.wasNull()) null else it }
                val kildeAlias = row.getString("tabellnavnalias_kilde")
                val spesialutbetaling = mapSpesialutbetaling(row)
                MeldekortPostering(
                    posteringId = row.getLong("postering_id"),
                    posteringTypeKode = row.getString("posteringtypekode"),
                    vedtakId = row.getInt("vedtak_id"),
                    personId = row.getInt("person_id"),
                    meldekortId = meldekortId,
                    periode = Periode(
                        fraOgMedDato = row.getDate("dato_periode_fra").toLocalDate(),
                        tilOgMedDato = row.getDate("dato_periode_til").toLocalDate(),
                    ),
                    belop = row.getInt("belop"),
                    antall = row.getDoubleOrNull("antall"),
                    dagsatsMedBarnetillegg = row.getString("dagsats_med_barnetillegg")?.toIntOrNull(),
                    dagsats = row.getString("dagsats")?.toIntOrNull(),
                    dagsatsForSamordning = row.getString("dagsats_for_samordning")?.toIntOrNull(),
                    insGrad = row.getString("ins_grad")?.toIntOrNull(),
                    kilde = PosteringKilde.fraKode(kildeAlias),
                    kildeAlias = kildeAlias,
                    kildeObjektId = kildeObjektId,
                    spesialutbetaling = spesialutbetaling,
                )
            }
        }

    // LEFT JOIN gir NULL i spes_id når posteringen ikke er en spesialutbetaling, eller når raden
    // i SPESIALUTBETALING mangler. Da finnes det ingen spesialutbetaling å vise.
    private fun mapSpesialutbetaling(row: ResultSet): Spesialutbetaling? {
        row.getLong("spes_id")
        if (row.wasNull()) return null
        return Spesialutbetaling(
            begrunnelse = row.getString("spes_begrunnelse"),
            belop = row.getDoubleOrNull("spes_belop"),
            belopKode = row.getString("spes_belopkode"),
            datoUtbetaling = row.getDate("spes_dato_utbetaling")?.toLocalDate(),
            periode = Periode(
                fraOgMedDato = row.getDate("spes_dato_fra")?.toLocalDate(),
                tilOgMedDato = row.getDate("spes_dato_til")?.toLocalDate(),
            ),
            vedtakStatusKode = row.getString("spes_vedtakstatuskode"),
            posteringTypeKode = row.getString("spes_posteringtypekode"),
            statusBilag = tilBoolean(row.getString("spes_status_bilag")),
            statusAnvistBilag = tilBoolean(row.getString("spes_status_anvis_bilag")),
            kategori = row.getString("spes_kategori"),
            valgtUtbetalingType = row.getString("spes_valgt_utbet_type"),
            saksbehandler = row.getString("spes_saksbehandler"),
            beslutter = row.getString("spes_beslutter"),
        )
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
        val anmerkningerPerMeldekort = selectAnmerkninger(sakId, meldekortIder, connection)

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
                reduksjon = reduksjonFra(anmerkninger),
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
        sakId: SakId,
        meldekortIder: List<Long>,
        connection: Connection,
    ): Map<Long, List<MeldekortAnmerkning>> {
        if (meldekortIder.isEmpty()) return emptyMap()
        return meldekortIder.chunked(chunkStørrelse).flatMap { chunk ->
            val sql = anmerkningerForMeldekortlisteSql(chunk)
            connection.createParameterizedQuery(sql).use { preparedStatement ->
                preparedStatement.setInt(1, sakId.id)
                preparedStatement.executeQuery().map { row ->
                    row.getLong("objekt_id") to MeldekortAnmerkning(
                        kode = row.getString("anmerkningkode"),
                        navn = row.getString("anmerkningnavn"),
                        beskrivelse = row.getString("beskrivelse"),
                        verdi = row.getIntOrNull("verdi"),
                        verdi2 = row.getIntOrNull("verdi2"),
                        vedtakId = row.getIntOrNull("vedtak_id"),
                    )
                }
            }
        }.groupBy({ it.first }, { it.second })
    }

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
        WITH fakta AS (
            SELECT vf.vedtak_id,
                   MAX(CASE WHEN vf.vedtakfaktakode = 'DAGSMBT'  THEN vf.vedtakverdi END) AS dagsats_med_barnetillegg,
                   MAX(CASE WHEN vf.vedtakfaktakode = 'DAGS'     THEN vf.vedtakverdi END) AS dagsats,
                   MAX(CASE WHEN vf.vedtakfaktakode = 'DAGSFSAM' THEN vf.vedtakverdi END) AS dagsats_for_samordning,
                   MAX(CASE WHEN vf.vedtakfaktakode = 'INSGRAD'  THEN vf.vedtakverdi END) AS ins_grad
              FROM vedtakfakta vf
             WHERE vf.vedtak_id IN (SELECT v.vedtak_id FROM vedtak v WHERE v.sak_id = ?)
               AND vf.vedtakfaktakode IN ('DAGSMBT', 'DAGS', 'DAGSFSAM', 'INSGRAD')
             GROUP BY vf.vedtak_id
        )
        SELECT p.postering_id, p.posteringtypekode, p.vedtak_id, p.person_id, p.meldekort_id, p.dato_periode_fra, p.dato_periode_til, p.belop,
               p.antall,
               p.tabellnavnalias_kilde, p.objekt_id_kilde,
               f.dagsats_med_barnetillegg, f.dagsats, f.dagsats_for_samordning, f.ins_grad,
               s.spesutbetaling_id AS spes_id, s.begrunnelse AS spes_begrunnelse, s.belop AS spes_belop,
               s.belopkode AS spes_belopkode, s.dato_utbetaling AS spes_dato_utbetaling,
               s.dato_fra AS spes_dato_fra, s.dato_til AS spes_dato_til,
               s.vedtakstatuskode AS spes_vedtakstatuskode, s.posteringtypekode AS spes_posteringtypekode,
               s.status_bilag AS spes_status_bilag, s.status_anvis_bilag AS spes_status_anvis_bilag,
               s.kategori AS spes_kategori, s.valgt_utbet_type AS spes_valgt_utbet_type,
               s.bruker_id_saksbehandler AS spes_saksbehandler, s.bruker_id_beslutter AS spes_beslutter
          FROM postering p
          JOIN vedtak v ON v.vedtak_id = p.vedtak_id
          LEFT JOIN fakta f ON f.vedtak_id = p.vedtak_id
          -- Aliaset må med i join-betingelsen fordi OBJEKT_ID_KILDE peker på ulike tabeller avhengig av kilde
          LEFT JOIN spesialutbetaling s ON s.spesutbetaling_id = p.objekt_id_kilde
                                       AND p.tabellnavnalias_kilde = 'SPESUTB'
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
    // Anmerkninger fra beregninger mot vedtak på andre saker hører ikke til denne saken.
    private fun anmerkningerForMeldekortlisteSql(meldekortIder: List<Long>): String {
        val idListe = meldekortIder.joinToString(",")
        return """
            SELECT a.objekt_id, a.anmerkningkode, a.verdi, a.verdi2, a.vedtak_id,
                   at.anmerkningnavn, at.beskrivelse
              FROM anmerkning a
              LEFT JOIN anmerkningtype at ON at.anmerkningkode = a.anmerkningkode
             WHERE a.tabellnavnalias = 'MKORT'
               AND a.objekt_id IN ($idListe)
               AND (a.vedtak_id IS NULL
                    OR a.vedtak_id IN (SELECT v.vedtak_id FROM vedtak v WHERE v.sak_id = ?))
             ORDER BY a.objekt_id, a.anmerkning_id
        """.trimIndent()
    }

    private companion object {
        private const val DAGER_PER_UKE = 7
    }
}




package arenaoppslag.aap.database

import java.sql.Date

object DbDato {
    fun fraDato(date: Date?) = date?.toLocalDate()
}

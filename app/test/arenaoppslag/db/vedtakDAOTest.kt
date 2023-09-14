package arenaoppslag.db


import arenaoppslag.db.InitTestDatabase.dataSource
import arenaoppslag.dao.VedtakDao
import org.junit.jupiter.api.Test
import java.time.LocalDate

class vedtakDAOTest() {
    private val vedtakDao = VedtakDao(dataSource)

    @Test
    fun `test db tilkobling`(){
        assert(vedtakDao.selectVedtakMedTidsbegrensning("1", LocalDate.ofYearDay(2022,1)).size==1)
    }
}
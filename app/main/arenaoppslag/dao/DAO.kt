package arenaoppslag.dao

import java.sql.Connection
import java.sql.ResultSet

private class ResultSetSequence(private val resultSet: ResultSet) : Sequence<ResultSet> {
    override fun iterator(): Iterator<ResultSet> {
        return ResultSetIterator()
    }

    private inner class ResultSetIterator : Iterator<ResultSet> {
        override fun hasNext(): Boolean {
            return resultSet.next()
        }

        override fun next(): ResultSet {
            return resultSet
        }
    }
}

fun <T : Any> ResultSet.map(block: (rs: ResultSet) -> T): Sequence<T> {
    return ResultSetSequence(this).map(block)
}

fun ResultSet.forEach(block: (rs: ResultSet) -> Unit) {
     ResultSetSequence(this).forEach(block)
}

fun <T> Connection.transaction(block: (connection: Connection) -> T): T {
    return this.use { connection ->
        try {
            connection.autoCommit = false
            val result = block(this)
            connection.commit()
            result
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
}

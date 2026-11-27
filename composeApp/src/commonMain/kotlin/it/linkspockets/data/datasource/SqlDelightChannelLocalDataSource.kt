package it.linkspockets.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.linkspockets.database.Canale
import kotlinx.coroutines.flow.Flow
import it.linkspockets.database.LinksDatabase
import it.linkspockets.domain.model.ChannelDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SqlDelightChannelLocalDataSource(
    database: LinksDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ChannelLocalDataSource {
    private val queries = database.canaleQueries

    override fun observeChannels(): Flow<List<Canale>> =
        queries.selectAll().asFlow().mapToList(dispatcher)

    override suspend fun insert(draft: ChannelDraft): Boolean = withContext(dispatcher) {
        // Keep the insert and changes() on the same connection in one transaction.
        queries.transactionWithResult {
            queries.insertIfAbsent(draft.nome, draft.url, draft.categoria.ifEmpty { null })
            queries.insertedRowCount().executeAsOne() > 0
        }
    }
}

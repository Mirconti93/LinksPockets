package it.linkspockets.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.linkspockets.database.Link
import it.linkspockets.database.LinksDatabase
import it.linkspockets.domain.model.LinkDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SqlDelightLinkLocalDataSource(
    database: LinksDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LinkLocalDataSource {
    private val queries = database.linkQueries

    override fun observeLinks(): Flow<List<Link>> =
        queries.selectAll().asFlow().mapToList(dispatcher)

    override suspend fun insert(draft: LinkDraft) {
        withContext(dispatcher) {
            queries.insertLink(
                nome = draft.nome,
                url = draft.url,
                canale = draft.canale,
                categoria = draft.categoria,
            )
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) {
            queries.deleteById(id)
        }
    }
}

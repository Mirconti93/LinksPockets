package it.linkspockets.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.linkspockets.database.SelectAll
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

    override fun observeLinks(): Flow<List<SelectAll>> =
        queries.selectAll().asFlow().mapToList(dispatcher)

    override suspend fun insert(draft: LinkDraft) {
        withContext(dispatcher) {
            queries.transaction {
                queries.insertLink(draft.nome, draft.url, draft.canale)
                val id = queries.lastInsertedId().executeAsOne()
                draft.categorie.forEachIndexed { index, category ->
                    queries.insertCategory(id, category, index.toLong())
                }
            }
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) {
            queries.transaction {
                // Explicit cleanup also works with SQLite foreign_keys disabled.
                queries.deleteCategories(id)
                queries.deleteById(id)
            }
        }
    }
}

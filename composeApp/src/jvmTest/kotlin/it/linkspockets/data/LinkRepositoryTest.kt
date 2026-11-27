package it.linkspockets.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.database.LinksDatabase
import it.linkspockets.data.datasource.SqlDelightLinkLocalDataSource
import it.linkspockets.di.databaseTestKoin
import it.linkspockets.domain.model.InvalidLinkException
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import java.nio.file.Files
import java.sql.SQLException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinkRepositoryTest {
    @Test
    fun insertsTrimsSortsAndDeletes() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            // Resolve the driver so Koin owns its lifecycle even if schema creation fails.
            application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val observeLinks = application.koin.get<ObserveLinksUseCase>()
            val addLink = application.koin.get<AddLinkUseCase>()
            val deleteLink = application.koin.get<DeleteLinkUseCase>()
            assertTrue(observeLinks().first().isEmpty())
            addLink(LinkDraft(" Kotlin ", " https://kotlinlang.org ", " Web ", listOf(" Dev ", "UI", "Dev", "")))
            addLink(LinkDraft("Compose", "https://jetbrains.com", "Blog", listOf("UI")))

            val links = observeLinks().first()
            assertEquals(listOf("Compose", "Kotlin"), links.map { it.nome })
            assertEquals("https://kotlinlang.org", links.last().url)
            assertEquals("Web", links.last().canale)
            assertEquals(listOf("Dev", "UI"), links.last().categorie)

            deleteLink(links.first().id)
            assertEquals(listOf("Kotlin"), observeLinks().first().map { it.nome })
            assertFailsWith<InvalidLinkException> { addLink(LinkDraft()) }
            assertEquals(1, observeLinks().first().size)
        } finally {
            application.close()
        }
    }

    @Test
    fun storesLinksWithEmptyOrWhitespaceChannel() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val addLink = application.koin.get<AddLinkUseCase>()
            addLink(LinkDraft("Kotlin", "https://kotlinlang.org", "", listOf("Dev")))
            addLink(LinkDraft("Compose", "https://jetbrains.com", "   ", listOf("UI")))

            val links = application.koin.get<ObserveLinksUseCase>()().first()
            assertEquals(2, links.size)
            assertTrue(links.all { it.canale.isEmpty() })
        } finally {
            application.close()
        }
    }

    @Test
    fun migratesExistingLinksAndAllowsEmptyChannel() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            application.koin.get<LinksDatabase>()
            driver.execute(null, """
                CREATE TABLE link (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL CHECK (length(trim(nome)) > 0),
                    url TEXT NOT NULL CHECK (length(trim(url)) > 0),
                    canale TEXT NOT NULL CHECK (length(trim(canale)) > 0),
                    categoria TEXT NOT NULL CHECK (length(trim(categoria)) > 0)
                )
            """.trimIndent(), 0)
            driver.execute(null, """
                INSERT INTO link(id, nome, url, canale, categoria)
                VALUES (42, 'Kotlin', 'https://kotlinlang.org', 'Web', 'Dev')
            """.trimIndent(), 0)

            assertEquals(4L, LinksDatabase.Schema.version)
            LinksDatabase.Schema.migrate(driver, 1, LinksDatabase.Schema.version)

            val observeLinks = application.koin.get<ObserveLinksUseCase>()
            assertEquals(
                listOf(LinkEntry(42, "Kotlin", "https://kotlinlang.org", "Web", listOf("Dev"))),
                observeLinks().first(),
            )
            application.koin.get<AddLinkUseCase>()(
                LinkDraft("Compose", "https://jetbrains.com", "", listOf("UI")),
            )
            val links = observeLinks().first()
            assertEquals(2, links.size)
            assertEquals("", links.first().canale)
            assertTrue(links.first().id > 42)
        } finally {
            application.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observesCompleteCategoryListsAndDeletesOnlyTheirAssociations() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            val database = application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val emissions = mutableListOf<List<LinkEntry>>()
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                application.koin.get<ObserveLinksUseCase>()().take(2).toList(emissions)
            }
            application.koin.get<AddLinkUseCase>()(
                LinkDraft("Kotlin", "https://kotlinlang.org", "", listOf("Dev", "UI")),
            )
            collection.join()
            assertTrue(emissions.first().isEmpty())
            assertEquals(listOf("Dev", "UI"), emissions.last().single().categorie)
            application.koin.get<AddLinkUseCase>()(
                LinkDraft("Compose", "https://jetbrains.com", "", listOf("UI")),
            )
            application.koin.get<DeleteLinkUseCase>()(emissions.last().single().id)
            val rows = database.linkQueries.selectAll().executeAsList()
            assertEquals(listOf("Compose"), rows.map { it.nome })
            assertEquals(listOf("UI"), rows.map { it.categoria })
            driver.executeQuery(null, "SELECT count(*) FROM link_categoria", { cursor ->
                cursor.next()
                assertEquals(1L, cursor.getLong(0))
                app.cash.sqldelight.db.QueryResult.Unit
            }, 0).value
        } finally {
            application.close()
        }
    }

    @Test
    fun rollsBackLinkAndCategoriesIfAnAssociationFails() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            LinksDatabase.Schema.create(driver)
            val database = LinksDatabase(driver)
            val source = SqlDelightLinkLocalDataSource(database)
            assertFailsWith<SQLException> {
                source.insert(LinkDraft("Kotlin", "https://kotlinlang.org", "", listOf("Dev", "Dev")))
            }
            assertTrue(database.linkQueries.selectAll().executeAsList().isEmpty())
            source.insert(LinkDraft("Kotlin", "https://kotlinlang.org", "", listOf("Dev", "UI")))
            assertEquals(2, database.linkQueries.selectAll().executeAsList().size)
        } finally {
            driver.close()
        }
    }

    @Test
    fun migratesVersionThreePreservingSharedCategoryNamesAndChannels() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            val database = application.koin.get<LinksDatabase>()
            driver.execute(null, """
                CREATE TABLE link (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL, url TEXT NOT NULL, canale TEXT NOT NULL,
                    categoria TEXT NOT NULL
                )
            """.trimIndent(), 0)
            LinksDatabase.Schema.migrate(driver, 2, 3)
            database.canaleQueries.insertCanale("Web", "https://example.com", "Channel category")
            driver.execute(null, """
                INSERT INTO link(id, nome, url, canale, categoria) VALUES
                (41, 'First', 'https://example.com/1', 'Web', 'Dev'),
                (42, 'Second', 'https://example.com/2', '', 'Dev')
            """.trimIndent(), 0)
            LinksDatabase.Schema.migrate(driver, 3, LinksDatabase.Schema.version)
            val links = application.koin.get<ObserveLinksUseCase>()().first()
            assertEquals(listOf(42L, 41L), links.map { it.id })
            assertTrue(links.all { it.categorie == listOf("Dev") })
            assertEquals("Web", links.last().canale)
            assertEquals("Channel category", database.canaleQueries.selectAll().executeAsOne().categoria)
        } finally {
            application.close()
        }
    }

    @Test
    fun persistsAfterReopeningDatabase() = runTest {
        val file = Files.createTempFile("links-pockets-", ".db")
        try {
            val driver = JdbcSqliteDriver("jdbc:sqlite:$file")
            val firstApplication = databaseTestKoin(driver)
            try {
                firstApplication.koin.get<LinksDatabase>()
                LinksDatabase.Schema.create(driver)
                firstApplication.koin.get<AddLinkUseCase>()(
                    LinkDraft("Kotlin", "https://kotlinlang.org", "Web", listOf("Dev", "UI")),
                )
            } finally {
                firstApplication.close()
            }
            val secondApplication = databaseTestKoin(JdbcSqliteDriver("jdbc:sqlite:$file"))
            try {
                val links = secondApplication.koin.get<ObserveLinksUseCase>()().first()
                assertEquals(1, links.size)
                assertEquals("Kotlin", links.single().nome)
                assertEquals(listOf("Dev", "UI"), links.single().categorie)
            } finally {
                secondApplication.close()
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

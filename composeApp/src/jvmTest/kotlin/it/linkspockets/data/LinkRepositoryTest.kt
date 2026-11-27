package it.linkspockets.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.database.LinksDatabase
import it.linkspockets.di.databaseTestKoin
import it.linkspockets.domain.model.InvalidLinkException
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import java.nio.file.Files
import kotlinx.coroutines.flow.first
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
            addLink(LinkDraft(" Kotlin ", " https://kotlinlang.org ", " Web ", " Dev "))
            addLink(LinkDraft("Compose", "https://jetbrains.com", "Blog", "UI"))

            val links = observeLinks().first()
            assertEquals(listOf("Compose", "Kotlin"), links.map { it.nome })
            assertEquals("https://kotlinlang.org", links.last().url)
            assertEquals("Web", links.last().canale)
            assertEquals("Dev", links.last().categoria)

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
            addLink(LinkDraft("Kotlin", "https://kotlinlang.org", "", "Dev"))
            addLink(LinkDraft("Compose", "https://jetbrains.com", "   ", "UI"))

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

            assertEquals(3L, LinksDatabase.Schema.version)
            LinksDatabase.Schema.migrate(driver, 1, LinksDatabase.Schema.version)

            val observeLinks = application.koin.get<ObserveLinksUseCase>()
            assertEquals(
                listOf(LinkEntry(42, "Kotlin", "https://kotlinlang.org", "Web", "Dev")),
                observeLinks().first(),
            )
            application.koin.get<AddLinkUseCase>()(
                LinkDraft("Compose", "https://jetbrains.com", "", "UI"),
            )
            val links = observeLinks().first()
            assertEquals(2, links.size)
            assertEquals("", links.first().canale)
            assertTrue(links.first().id > 42)
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
                    LinkDraft("Kotlin", "https://kotlinlang.org", "Web", "Dev"),
                )
            } finally {
                firstApplication.close()
            }
            val secondApplication = databaseTestKoin(JdbcSqliteDriver("jdbc:sqlite:$file"))
            try {
                val links = secondApplication.koin.get<ObserveLinksUseCase>()().first()
                assertEquals(1, links.size)
                assertEquals("Kotlin", links.single().nome)
            } finally {
                secondApplication.close()
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

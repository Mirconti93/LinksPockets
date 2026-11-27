package it.linkspockets.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.database.LinksDatabase
import java.nio.file.Files
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanaleDatabaseTest {
    @Test
    fun insertsSortsAndDeletesWithOptionalCategory() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            LinksDatabase.Schema.create(driver)
            val queries = LinksDatabase(driver).canaleQueries
            queries.insertCanale("Kotlin", "https://kotlinlang.org", "Dev")
            queries.insertCanale("Android", "https://developer.android.com", null)

            val channels = queries.selectAll().executeAsList()
            assertEquals(listOf("Android", "Kotlin"), channels.map { it.nome })
            assertNull(channels.first().categoria)
            assertEquals("https://kotlinlang.org", channels.last().url)
            assertEquals("Dev", channels.last().categoria)
            assertTrue(channels.all { it.id > 0 })

            queries.deleteById(channels.first().id)
            assertEquals(listOf("Kotlin"), queries.selectAll().executeAsList().map { it.nome })
        } finally {
            driver.close()
        }
    }

    @Test
    fun rejectsMissingNamesUrlsAndDuplicateNames() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            LinksDatabase.Schema.create(driver)
            val queries = LinksDatabase(driver).canaleQueries
            queries.insertCanale("Kotlin", "https://kotlinlang.org", null)

            assertFailsWith<SQLException> {
                queries.insertCanale("Kotlin", "https://example.com", "Other")
            }
            listOf("", "   ").forEach { blank ->
                assertFailsWith<SQLException> { queries.insertCanale(blank, "https://example.com", null) }
                assertFailsWith<SQLException> { queries.insertCanale("Android", blank, null) }
            }
            assertFailsWith<SQLException> {
                driver.execute(null, "INSERT INTO canale(nome, url) VALUES (NULL, 'https://example.com')", 0)
            }
            assertFailsWith<SQLException> {
                driver.execute(null, "INSERT INTO canale(nome, url) VALUES ('Android', NULL)", 0)
            }
            assertEquals(1, queries.selectAll().executeAsList().size)
        } finally {
            driver.close()
        }
    }

    @Test
    fun migratesVersionTwoWithoutChangingExistingLinks() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null, """
                CREATE TABLE link (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL CHECK (length(trim(nome)) > 0),
                    url TEXT NOT NULL CHECK (length(trim(url)) > 0),
                    canale TEXT NOT NULL,
                    categoria TEXT NOT NULL CHECK (length(trim(categoria)) > 0)
                )
            """.trimIndent(), 0)
            driver.execute(null, """
                INSERT INTO link(id, nome, url, canale, categoria)
                VALUES (42, 'Kotlin', 'https://kotlinlang.org', 'Web', 'Dev')
            """.trimIndent(), 0)

            LinksDatabase.Schema.migrate(driver, 2, LinksDatabase.Schema.version)
            val database = LinksDatabase(driver)
            val link = database.linkQueries.selectAll().executeAsOne()
            assertEquals(42L, link.id)
            assertEquals("Kotlin", link.nome)
            assertEquals("https://kotlinlang.org", link.url)
            assertEquals("Web", link.canale)
            assertEquals("Dev", link.categoria)
            assertTrue(database.canaleQueries.selectAll().executeAsList().isEmpty())

            database.canaleQueries.insertCanale("Kotlin", "https://kotlinlang.org", null)
            assertEquals("Kotlin", database.canaleQueries.selectAll().executeAsOne().nome)
            assertFailsWith<SQLException> {
                database.canaleQueries.insertCanale("Kotlin", "https://example.com", null)
            }
        } finally {
            driver.close()
        }
    }

    @Test
    fun persistsAfterReopeningDatabase() {
        val file = Files.createTempFile("links-pockets-canale-", ".db")
        try {
            val firstDriver = JdbcSqliteDriver("jdbc:sqlite:$file")
            try {
                LinksDatabase.Schema.create(firstDriver)
                LinksDatabase(firstDriver).canaleQueries.insertCanale("Kotlin", "https://kotlinlang.org", "Dev")
            } finally {
                firstDriver.close()
            }
            val secondDriver = JdbcSqliteDriver("jdbc:sqlite:$file")
            try {
                val channel = LinksDatabase(secondDriver).canaleQueries.selectAll().executeAsOne()
                assertEquals("Kotlin", channel.nome)
                assertEquals("https://kotlinlang.org", channel.url)
                assertEquals("Dev", channel.categoria)
            } finally {
                secondDriver.close()
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

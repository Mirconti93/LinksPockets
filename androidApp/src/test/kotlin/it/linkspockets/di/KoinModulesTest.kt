package it.linkspockets.di

import androidx.lifecycle.ViewModelStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.data.datasource.LinkLocalDataSource
import it.linkspockets.data.settings.MemorySharedPreferences
import it.linkspockets.data.settings.SharedPreferencesThemeRepository
import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository
import it.linkspockets.presentation.settings.SettingsIntent
import it.linkspockets.presentation.settings.SettingsViewModel
import it.linkspockets.database.LinksDatabase
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.repository.LinkRepository
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import it.linkspockets.domain.usecase.ValidateLinkUseCase
import it.linkspockets.presentation.channel.ChannelEffect
import kotlinx.coroutines.flow.first
import it.linkspockets.presentation.channel.ChannelIntent
import it.linkspockets.presentation.channel.ChannelViewModel
import it.linkspockets.presentation.link.LinkIntent
import it.linkspockets.presentation.link.LinkViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KoinModulesTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun resolvesFullGraphWithSingletonDataAndFactoryUseCasesAndViewModels() = runTest(dispatcher) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver, module {
            single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
        })
        try {
            val koin = application.koin
            val database = koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            assertSame(database, koin.get<LinksDatabase>())
            assertSame(koin.get<LinkLocalDataSource>(), koin.get<LinkLocalDataSource>())
            assertSame(koin.get<LinkRepository>(), koin.get<LinkRepository>())
            assertNotSame(koin.get<AddLinkUseCase>(), koin.get<AddLinkUseCase>())
            koin.get<ObserveLinksUseCase>()
            koin.get<DeleteLinkUseCase>()
            koin.get<ValidateLinkUseCase>()
            val first = koin.get<LinkViewModel>()
            store.put("first", first)
            val second = koin.get<LinkViewModel>()
            store.put("second", second)
            assertNotSame(first, second)
            runCurrent()
            assertFalse(first.state.value.isLoading)
            assertFalse(first.state.value.hasLoadError)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun injectsUseCasesUsingOverriddenRepositoryAndDispatcher() = runTest(dispatcher) {
        val repository = FakeLinkRepository()
        val application = databaseTestKoin(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY),
            module {
                single<LinkRepository> { repository }
                single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
            },
        )
        try {
            // Link persistence is fake; channel observation still uses the real database.
            application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(application.koin.get<SqlDriver>())
            assertSame(dispatcher, application.koin.get<CoroutineDispatcher>(databaseDispatcherQualifier))
            val viewModel = application.koin.get<LinkViewModel>()
            store.put("links", viewModel)
            viewModel.dispatch(LinkIntent.ShowAddForm)
            viewModel.dispatch(LinkIntent.NameChanged(" Kotlin "))
            viewModel.dispatch(LinkIntent.UrlChanged(" https://kotlinlang.org "))
            viewModel.dispatch(LinkIntent.ChannelChanged(" Web "))
            viewModel.dispatch(LinkIntent.CategoryChanged(" Dev "))
            viewModel.dispatch(LinkIntent.SaveLink)
            runCurrent()
            assertEquals(listOf(LinkDraft("Kotlin", "https://kotlinlang.org", "Web", listOf("Dev"))), repository.added)
            assertNull(viewModel.state.value.form)
            assertEquals("Kotlin", viewModel.state.value.links.single().nome)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun channelFormSavesThroughFullGraphAndReportsDuplicateNames() = runTest(dispatcher) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver, module {
            single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
        })
        try {
            val database = application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val viewModel = application.koin.get<ChannelViewModel>()
            store.put("channel", viewModel)
            viewModel.dispatch(ChannelIntent.ShowForm)
            viewModel.dispatch(ChannelIntent.NameChanged(" Kotlin "))
            viewModel.dispatch(ChannelIntent.UrlChanged(" https://kotlinlang.org "))
            viewModel.dispatch(ChannelIntent.Save)
            runCurrent()
            assertNull(viewModel.state.value.form)
            val channel = database.canaleQueries.selectAll().executeAsOne()
            assertEquals("Kotlin", channel.nome)
            assertEquals("https://kotlinlang.org", channel.url)
            assertNull(channel.categoria)

            viewModel.dispatch(ChannelIntent.ShowForm)
            viewModel.dispatch(ChannelIntent.NameChanged("Kotlin"))
            viewModel.dispatch(ChannelIntent.UrlChanged("https://example.com"))
            viewModel.dispatch(ChannelIntent.Save)
            runCurrent()
            assertEquals("Esiste già un canale con questo nome.", viewModel.state.value.form?.error)
            assertEquals(1, database.canaleQueries.selectAll().executeAsList().size)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun createsChannelFromLinkAndSavesTheLinkWithItsSelection() = runTest(dispatcher) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver, module {
            single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
        })
        try {
            val database = application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val linkViewModel = application.koin.get<LinkViewModel>()
            val channelViewModel = application.koin.get<ChannelViewModel>()
            store.put("links", linkViewModel)
            store.put("channel", channelViewModel)
            linkViewModel.dispatch(LinkIntent.ShowAddForm)
            linkViewModel.dispatch(LinkIntent.NameChanged("Article"))
            linkViewModel.dispatch(LinkIntent.UrlChanged("https://www.example.com/article?q=1"))
            linkViewModel.dispatch(LinkIntent.CategoryChanged("Reading"))
            runCurrent()
            val draft = linkViewModel.state.value.form?.draft

            channelViewModel.dispatch(ChannelIntent.ShowFormForLink(draft?.url.orEmpty()))
            runCurrent()
            assertEquals("www.example.com", channelViewModel.state.value.form?.draft?.nome)
            assertEquals("https://www.example.com", channelViewModel.state.value.form?.draft?.url)
            channelViewModel.dispatch(ChannelIntent.DismissForm)
            runCurrent()
            assertEquals(draft, linkViewModel.state.value.form?.draft)
            assertTrue(database.canaleQueries.selectAll().executeAsList().isEmpty())

            channelViewModel.dispatch(ChannelIntent.ShowFormForLink(draft?.url.orEmpty()))
            channelViewModel.dispatch(ChannelIntent.Save)
            runCurrent()
            val saved = channelViewModel.effect.first() as ChannelEffect.Saved
            assertTrue(saved.forLink)
            // Same forwarding performed by LinkRoute after the channel is saved.
            linkViewModel.dispatch(LinkIntent.ChannelChanged(saved.name))
            runCurrent()
            assertEquals(listOf(saved.name), linkViewModel.state.value.channels.map { it.nome })
            assertEquals(draft?.copy(canale = saved.name), linkViewModel.state.value.form?.draft)
            linkViewModel.dispatch(LinkIntent.SaveLink)
            runCurrent()
            val link = database.linkQueries.selectAll().executeAsOne()
            assertEquals("Article", link.nome)
            assertEquals("https://www.example.com/article?q=1", link.url)
            assertEquals("www.example.com", link.canale)
            assertEquals("Reading", link.categoria)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun settingsGraphAppliesAndRestoresSavedTheme() = runTest(dispatcher) {
        val preferences = MemorySharedPreferences()
        val application = databaseTestKoin(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY), module {
            single<ThemeRepository> { SharedPreferencesThemeRepository(preferences, dispatcher) }
        })
        try {
            application.koin.get<LinksDatabase>()
            val first = application.koin.get<SettingsViewModel>()
            store.put("settings-first", first)
            first.dispatch(SettingsIntent.Open)
            first.dispatch(SettingsIntent.ThemeSelected(AppTheme.DARK))
            runCurrent()
            assertEquals(AppTheme.DARK, first.state.value.theme)
            assertEquals(1, preferences.commits)
            val second = application.koin.get<SettingsViewModel>()
            store.put("settings-second", second)
            runCurrent()
            assertNotSame(first, second)
            assertEquals(AppTheme.DARK, second.state.value.theme)
            assertFalse(second.state.value.isOpen)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun closesResolvedDatabaseDriverWithKoinApplication() {
        val driver = TrackingSqlDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        val application = databaseTestKoin(driver)
        try {
            application.koin.get<LinksDatabase>()
            assertFalse(driver.closed)
        } finally {
            application.close()
        }
        assertTrue(driver.closed)
    }

    private class TrackingSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
        var closed = false
        override fun close() {
            closed = true
            delegate.close()
        }
    }

    private class FakeLinkRepository : LinkRepository {
        val added = mutableListOf<LinkDraft>()
        private val links = MutableStateFlow<List<LinkEntry>>(emptyList())

        override fun observeLinks(): Flow<List<LinkEntry>> = links
        override suspend fun add(draft: LinkDraft) {
            added += draft
            links.value = listOf(LinkEntry(1, draft.nome, draft.url, draft.canale, draft.categorie))
        }
        override suspend fun delete(id: Long) {
            links.value = links.value.filterNot { it.id == id }
        }
    }
}

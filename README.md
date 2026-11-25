# LinksPockets

App Android per salvare link in un database SQLite locale, senza backend o account.
UI Compose con MVI; data e domain in una libreria Kotlin Multiplatform.

## Funzionalità

- Lista dei link, dal più recente al meno recente.
- Inserimento con nome, URL, canale e categoria obbligatori.
- Validazione di base degli URL HTTP/HTTPS e trim dei campi.
- Apertura dei link nel browser di sistema.
- Eliminazione con conferma.
- Persistenza nel file `links.db`, nella sandbox dell'app, anche dopo la chiusura.
- Gestione degli errori e retry, con blocco dei tap duplicati durante le operazioni.

## Moduli

```text
composeApp/src/androidMain/               # Modulo Gradle :androidMain, app Android
  src/main/
    AndroidManifest.xml
    kotlin/it/linkspockets/
      MainActivity.kt                     # Application e Activity
      di/
        AndroidModule.kt                  # Driver SQLite Android
        InitKoin.kt                       # Avvio del grafo Koin
        PresentationModule.kt             # Registrazione del ViewModel
      presentation/
        App.kt                            # Tema e koinViewModel()
        BaseViewModel.kt                  # Classi base presentation
        LinkValidationMessages.kt         # Errori domain → messaggi UI
        link/
          LinkIntent.kt
          LinkState.kt
          LinkEffect.kt
          LinkViewModel.kt
          LinkRoute.kt
          LinkScreen.kt
  src/test/kotlin/                        # Test MVI e wiring Koin completo
  build.gradle.kts

composeApp/                               # Modulo Gradle :composeApp, libreria KMP
  src/commonMain/
    kotlin/it/linkspockets/
      data/
        datasource/                       # Accesso SQLDelight
        mapper/                           # Record SQLite → modelli domain
        repository/                       # Implementazione LinkRepository
      domain/
        model/                            # LinkEntry, LinkDraft, errori
        repository/                       # Contratto LinkRepository
        usecase/                          # Observe, Add, Delete, Validate
      di/
        DataModule.kt                     # Database, datasource, repository, dispatcher
        DomainModule.kt                   # Use case
    sqldelight/it/linkspockets/database/Link.sq
  src/iosMain/                            # Driver iOS disponibile per uso futuro
  src/commonTest/                         # Test domain senza DB
  src/jvmTest/                            # Test SQLite reale e persistenza
  build.gradle.kts
```

`androidMain` è il nome di un **modulo Gradle indipendente**, non di un source set
Multiplatform. È collocato fisicamente in `composeApp/src/androidMain/`, accanto
a `composeApp/src/iosMain/`, con percorso configurato in `settings.gradle.kts`.
Il modulo applicazione usa i source set Android standard `src/main`
e `src/test`. Il nome Gradle resta `:androidMain`, quindi i comandi non cambiano.
Dipende da `composeApp`, che non dipende dall'app né da Compose o AndroidX Lifecycle.

La UI è **solo Android**. L'entry point SwiftUI e il progetto Xcode dell'app sono
stati rimossi; restano i target iOS ARM64 e simulatore ARM64 della libreria condivisa,
senza UI. Per riaggiungere un'app iOS sarà necessario creare una presentation dedicata.

## Database

La tabella `link` contiene `nome`, `url`, `canale`, `categoria` (TEXT NOT NULL)
più `id` (INTEGER PRIMARY KEY AUTOINCREMENT). Schema e query:

`composeApp/src/commonMain/sqldelight/it/linkspockets/database/Link.sq`.

Lo spostamento della UI non cambia schema, nome del database o application ID
Android (`it.linkspockets`), mantenendo compatibili i dati delle installazioni esistenti.
Il backup Android è disabilitato. Il database non richiede permessi di rete;
i link vengono aperti dal browser esterno.

## Clean Architecture

- **Domain**: modelli, contratto del repository e use case; nessuna dipendenza
  da Koin, Compose, SQLDelight o presentation. Usa `Flow` di Kotlin Coroutines.
- **Data**: datasource SQLDelight sul dispatcher iniettato; `LinkRepositoryImpl`
  implementa il contratto domain e converte i record del DB in modelli domain.
- **Presentation**: nel modulo Android. Il ViewModel invoca gli use case,
  traduce gli errori e gestisce lo stato. I composable renderizzano e inviano intent.
- **DI**: moduli Koin condivisi per data/domain, moduli Android per driver e ViewModel.

Flusso delle dipendenze: `androidMain → composeApp`.
Validazione e normalizzazione dei campi restano negli use case.

## Dependency injection con Koin

Koin 4.1.1 usa constructor injection:

- `dataModule`: database, datasource e repository singleton; dispatcher qualificato
  con `databaseDispatcherQualifier` e sostituibile nei test.
- `domainModule`: factory dei quattro use case.
- `presentationModule`: registrazione del `LinkViewModel` con la DSL `viewModel`;
  i quattro use case sono iniettati direttamente nel costruttore.
- `androidModule(context)`: singleton del driver SQLite, con application context;
  il driver risolto viene chiuso alla chiusura del container.
- `initKoin` viene chiamato una sola volta in `Application.onCreate`.

La UI recupera il ViewModel tramite il `ViewModelStore` della schermata:

```kotlin
val viewModel: LinkViewModel = koinViewModel()
LinkRoute(viewModel)
```

Domain e ViewModel non usano le API Koin internamente.

## Presentation MVI

```text
LinkScreen → LinkIntent → LinkViewModel → use case
                              ↓
                         LinkState → LinkScreen
                              ↓
                         LinkEffect → LinkRoute
```

- `LinkState`: fonte unica di verità, esposta come `StateFlow` di sola lettura;
  contiene lista, caricamento, errore di lettura, form e conferma di eliminazione.
- `LinkIntent`: azioni dell'utente, comprese modifica dei campi, salvataggio,
  conferma/annullamento dell'eliminazione, apertura link e retry.
- `LinkViewModel`: estende `BaseEffectViewModel<LinkIntent, LinkState, LinkEffect>`
  e implementa `onAction`. Stato ed effetti sono gestiti dalla base tramite
  `updateState` ed `emitEffect`, senza flow o canali duplicati nel ViewModel.
  La UI invia intent con `dispatch`, eseguito su `Dispatchers.Main.immediate`;
  il datasource continua a eseguire le query sul proprio dispatcher.
  `viewModelScope` cancella le operazioni quando il ViewModel viene rimosso.
  Il form sopravvive alle rotazioni, ma non alla terminazione del processo.
- `LinkEffect`: snackbar e apertura URL one-shot, trasmessi tramite un `Channel`
  buffered a un solo consumer. Gli effetti non ancora consumati restano in coda
  finché il ViewModel è vivo; non sono persistiti.
- `LinkRoute`: raccoglie lo stato con `collectAsStateWithLifecycle` e consuma
  il flow `effect` della base quando il lifecycle è almeno `STARTED`.
  La base chiude il canale degli effetti in `onCleared`.
- `LinkScreen`: stateless salvo lo stato visivo di scorrimento dei composable.

Durante salvataggio/eliminazione, tap duplicati e dismiss vengono ignorati.
Gli errori conservano il form o la conferma di eliminazione e consentono il retry.

## Build e avvio Android

Requisiti: JDK **17**, Android SDK 35 e Android Studio. Impostare JDK 17 come
Gradle JDK: Gradle 8.11.1 non è compatibile con JDK 25.

1. Aprire la cartella principale in Android Studio.
2. Configurare l'SDK tramite `local.properties` (non versionato) o `ANDROID_HOME`.
3. Sincronizzare Gradle ed eseguire il modulo **`androidMain`** su dispositivo/emulatore
   Android 7.0 o successivo (API 24).

Da terminale su macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :androidMain:assembleDebug
# Con dispositivo/emulatore già collegato:
./gradlew :androidMain:installDebug
```

APK: `composeApp/src/androidMain/build/outputs/apk/debug/androidMain-debug.apk`.
`composeApp` è una libreria e non ha più task `installDebug`.

## Test

```bash
# Domain e persistenza SQLite della libreria condivisa:
./gradlew :composeApp:jvmTest
# MVI e wiring Koin dell'app Android (test locali JVM):
./gradlew :androidMain:testDebugUnitTest
```

I test domain usano un repository fake. I test SQLite verificano inserimento,
mapping, ordinamento, eliminazione e persistenza dopo la riapertura del database.
Il target JVM della libreria serve per questi test, non è un'app desktop.

I test MVI verificano caricamento/retry, validazione, salvataggio, eliminazione,
blocchi durante le operazioni, effetti one-shot e cancellazione dell'osservazione.
I test DI verificano il grafo Koin completo, l'iniezione di repository/dispatcher
sostitutivi e la chiusura del driver. Usano `koinApplication` isolato, senza contesto globale.

## Evoluzione dello schema

Per modifiche successive al primo rilascio, aggiungere migrazioni SQLDelight `.sqm`
accanto a `Link.sq`, anziché modificare soltanto il `CREATE TABLE`, per conservare
correttamente i dati già presenti sui dispositivi.

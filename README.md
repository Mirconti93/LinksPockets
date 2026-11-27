# LinksPockets

App Android per salvare link in un database SQLite locale, senza backend o account.
UI Android Compose con MVI; data e domain in una libreria Kotlin Multiplatform.
Il modulo iOS espone una schermata UIKit vuota, senza ancora un'app host Xcode.

## Funzionalità

- Lista dei link, dal più recente al meno recente.
- Inserimento link con nome, URL e categoria obbligatori; canale facoltativo,
  selezionabile da un menu a tendina dei canali salvati (oppure **Nessun canale**).
- Creazione di un canale dal form link con **+ Nuovo canale**, senza perdere gli
  input del link, e selezione automatica del canale appena salvato.
- Form **+ Canale** nella barra superiore: nome univoco e URL HTTP/HTTPS obbligatori,
  categoria facoltativa, conferma al salvataggio e gestione dei duplicati.
- Validazione di base degli URL HTTP/HTTPS e trim dei campi.
- Apertura dei link nel browser di sistema.
- Eliminazione con conferma.
- Menu **⋮ → Impostazioni → Tema** nella barra superiore per scegliere
  **Chiaro (Light mode)** o **Scuro (Dark mode)**, con preferenza salvata.
- Persistenza nel file `links.db`, nella sandbox dell'app, anche dopo la chiusura.
- Gestione degli errori e retry, con blocco dei tap duplicati durante le operazioni.

## Moduli

```text
androidApp/                               # Modulo Gradle :androidApp, app Android
  src/main/
    AndroidManifest.xml
    kotlin/it/linkspockets/
      MainActivity.kt                     # Application, Activity e barre di sistema
      data/settings/
        SharedPreferencesThemeRepository.kt # Preferenza tema Android
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
          ChannelSelector.kt              # Menu canali salvati e creazione dal link
        channel/
          ChannelState.kt                 # Stato, intent ed effetto di conferma
          ChannelViewModel.kt             # Validazione e salvataggio MVI
          AddChannelDialog.kt             # Form Nuovo canale
        settings/
          SettingsMenu.kt                 # Menu con i tre puntini
          SettingsDialog.kt               # Sezione Impostazioni e scelta tema
          SettingsState.kt
          SettingsViewModel.kt
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
  src/commonTest/                         # Test domain senza DB
  src/jvmTest/                            # Test SQLite reale e persistenza
  build.gradle.kts

iosApp/                                   # Modulo Gradle :iosApp, libreria KMP per iOS
  src/iosMain/kotlin/it/linkspockets/
    di/IosModule.kt                       # Driver SQLite iOS
    presentation/MainViewController.kt    # Schermata UIKit vuota
  build.gradle.kts                        # Framework LinksPockets per device/simulatore
```

`androidApp`, `iosApp` e `composeApp` sono moduli Gradle indipendenti nella root.
L'app Android usa i source set standard `src/main` e `src/test`.
`iosApp` è una libreria Kotlin Multiplatform con target iOS ARM64 e simulatore ARM64:
il suo `src/iosMain` è un source set standard KMP, non un modulo separato.
Entrambi i moduli di piattaforma dipendono da `composeApp`, mai il contrario.
La libreria condivisa mantiene i target iOS per fornire data/domain a `iosApp`,
ma non produce più un framework: il framework `LinksPockets` è prodotto da `iosApp`.

Le funzionalità di gestione link restano disponibili solo su Android.
Su iOS `MainViewController()` crea una schermata UIKit vuota con sfondo adattivo
chiaro/scuro. Non viene ancora avviato Koin né aperto il database dalla schermata.
Il driver iOS è disponibile in `iosModule` per il futuro wiring dell'app.

## Impostazioni e tema

Il menu con i tre puntini nella **barra dell'app** apre la voce **Impostazioni**.
La scelta fra tema chiaro e scuro applica subito i colori Material 3 a schermata,
form e dialog e aggiorna l'aspetto delle barre di sistema Android. Non cambia
il tema globale del dispositivo. Per preservare il comportamento precedente,
il tema predefinito è chiaro.

La preferenza è salvata nelle SharedPreferences private `settings`, chiave
`app_theme`, e viene ripristinata all'avvio senza modificare `links.db`.
`SettingsViewModel` usa `ObserveThemeUseCase` e `SetThemeUseCase`; il repository
Android esegue il salvataggio sul dispatcher IO iniettato. In caso di errore
resta attivo il tema precedente e la scelta può essere ripetuta. Valori mancanti
o non riconosciuti vengono trattati come tema chiaro. I test JVM del repository
usano un test double di SharedPreferences; non richiedono un dispositivo Android.

## Database

La tabella `link` contiene `nome`, `url`, `canale`, `categoria` (TEXT NOT NULL)
più `id` (INTEGER PRIMARY KEY AUTOINCREMENT). Schema e query:

`composeApp/src/commonMain/sqldelight/it/linkspockets/database/Link.sq`.

La tabella `canale`, definita in `Canale.sq` nella stessa directory, contiene:

- `id`: chiave primaria automatica.
- `nome`: obbligatorio, non vuoto e univoco (confronto SQLite case-sensitive).
- `url`: obbligatorio e non vuoto.
- `categoria`: facoltativa, nullable.

Sono disponibili query SQLDelight per inserimento, lettura ordinata per nome ed
eliminazione. La migrazione `2.sqm` aggiunge la tabella ai database esistenti
(versione schema 3), senza modificare i link. Il campo testuale `link.canale`
resta testuale, senza introdurre una foreign key: il form link memorizza il nome
scelto dal menu, mantenendo compatibili i link esistenti. Il form Android
**+ Canale** salva tramite `AddChannelUseCase`,
normalizzando gli spazi e salvando la categoria vuota come NULL. I duplicati
mostrano un errore senza modificare il canale esistente. Durante il salvataggio
sono bloccati tap ripetuti, modifiche e chiusura del form; in caso di errore gli
input sono conservati per la correzione o il retry. Non sono ancora presenti
una schermata lista dedicata, modifica o eliminazione dei canali nella UI.

Il menu del form link osserva i canali salvati e si aggiorna automaticamente;
caricamento ed errori hanno uno stato dedicato e un retry che conserva il form.
Con **+ Nuovo canale**, `CreateChannelDraftFromLinkUseCase` suggerisce nome e URL
dal dominio del link: `https://www.esempio.it/articolo?q=1` diventa nome
`www.esempio.it` e URL `https://www.esempio.it`. Mantiene schema, sottodominio ed
un'eventuale porta nell'URL, escludendo credenziali, percorso, query e frammento.
Senza URL HTTP/HTTPS interpretabile i campi restano vuoti. I suggerimenti sono
modificabili; annullando si torna al link invariato, salvando si seleziona il
nuovo canale. La categoria del canale resta facoltativa.

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

Flusso delle dipendenze: `androidApp → composeApp ← iosApp`.
Validazione e normalizzazione dei campi restano negli use case.

## Dependency injection con Koin

Koin 4.1.1 usa constructor injection:

- `dataModule`: database, datasource e repository singleton; dispatcher qualificato
  con `databaseDispatcherQualifier` e sostituibile nei test.
- `domainModule`: factory degli use case per link, canali e preferenza tema.
- `presentationModule`: registrazione di `LinkViewModel`, `ChannelViewModel` e
  `SettingsViewModel` con la DSL `viewModel`; gli use case sono iniettati nei costruttori.
- `androidModule(context)`: singleton del driver SQLite e di `ThemeRepository`,
  con application context; il driver viene chiuso alla chiusura del container.
- `initKoin` viene chiamato una sola volta in `Application.onCreate`.

La UI recupera il ViewModel tramite il `ViewModelStore` della schermata:

```kotlin
val viewModel: LinkViewModel = koinViewModel()
val channelViewModel: ChannelViewModel = koinViewModel()
val settingsViewModel: SettingsViewModel = koinViewModel()
LinkRoute(viewModel, channelViewModel) {
    settingsViewModel.dispatch(SettingsIntent.Open)
}
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
3. Sincronizzare Gradle ed eseguire il modulo **`androidApp`** su dispositivo/emulatore
   Android 7.0 o successivo (API 24).

Da terminale su macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :androidApp:assembleDebug
# Con dispositivo/emulatore già collegato:
./gradlew :androidApp:installDebug
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
`composeApp` è una libreria e non ha più task `installDebug`.

## Framework e schermata iOS

Requisiti: JDK **17** e Xcode completo, con SDK iOS e toolchain selezionato
tramite `xcode-select` (le sole Command Line Tools non bastano).

```bash
# Framework per simulatore Apple Silicon:
./gradlew :iosApp:linkDebugFrameworkIosSimulatorArm64
# Framework per dispositivo:
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

Il framework si trova in
`iosApp/build/bin/iosSimulatorArm64/debugFramework/LinksPockets.framework`
(o `iosArm64` per dispositivo). Da Swift il controller è accessibile con:

```swift
import LinksPockets

let controller = MainViewControllerKt.MainViewController()
```

Non è incluso un progetto Xcode eseguibile: il modulo `:iosApp` produce un framework,
non un bundle `.app`. Per visualizzare la schermata serve un'app host iOS che
usi il controller come root view controller o tramite `UIViewControllerRepresentable`.
Per integrarlo in una build phase Xcode usare
`./gradlew :iosApp:embedAndSignAppleFrameworkForXcode` dalla root del repository.

## Test

```bash
# Domain e persistenza SQLite della libreria condivisa:
./gradlew :composeApp:jvmTest
# MVI e wiring Koin dell'app Android (test locali JVM):
./gradlew :androidApp:testDebugUnitTest
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

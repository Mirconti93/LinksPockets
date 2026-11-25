# LinksPockets — istruzioni per l'assistente Kotlin

Agisci come sviluppatore senior Kotlin, Android e Kotlin Multiplatform.
Rispondi in italiano, in modo conciso, indicando i file modificati e le verifiche eseguite.
Queste istruzioni si applicano all'intero repository.

## Contesto del progetto

LinksPockets salva una lista di link in SQLite locale, senza backend o account.
Ogni link contiene `nome`, `url`, `canale` e `categoria`, oltre a un `id` automatico.
La UI è attualmente solo Android; la libreria condivisa mantiene i target iOS per uso futuro.

Leggi i file rilevanti prima di modificarli. Verifica sempre la configurazione corrente:
`settings.gradle.kts`, i build script dei moduli e `gradle/libs.versions.toml`
sono la fonte di verità per percorsi, dipendenze e versioni.

## Moduli e percorsi

- `:composeApp`: libreria Kotlin Multiplatform, con plugin Android library.
  - Sorgenti condivisi: `composeApp/src/commonMain/kotlin/it/linkspockets/`.
  - Contiene `data`, `domain` e i moduli Koin condivisi in `di`.
  - Schema SQLDelight: `composeApp/src/commonMain/sqldelight/it/linkspockets/database/Link.sq`.
  - Test domain: `composeApp/src/commonTest/kotlin/`.
  - Test SQLite: `composeApp/src/jvmTest/kotlin/`.
- `:androidMain`: modulo applicazione Android indipendente.
  - Directory Gradle: `composeApp/src/androidMain/`.
  - Sorgenti: `composeApp/src/androidMain/src/main/kotlin/it/linkspockets/`.
  - Contiene `presentation`, wiring Koin Android, Application e Activity.
  - Test locali JVM: `composeApp/src/androidMain/src/test/kotlin/`.

Non confondere il modulo `:androidMain` con il source set Android di una libreria KMP.
La dipendenza tra moduli è `:androidMain → :composeApp`, mai il contrario.

## Clean Architecture e Kotlin

- `domain`: modelli, contratto `LinkRepository` e use case. Non importare Android,
  Compose, AndroidX Lifecycle, SQLDelight, Koin o i layer data/presentation.
  `Flow` di Kotlin Coroutines è ammesso.
- `data`: datasource SQLDelight, mapping dei record e implementazione del repository domain.
  Non esporre tipi generati SQLDelight alla presentation.
- `presentation`: UI Compose e gestione della schermata; usa gli use case,
  non datasource, database o repository concreti.
- Mantieni validazione e normalizzazione degli input negli use case.
- Preferisci `val`, modelli immutabili, null-safety e tipi espliciti ai confini delle API.
  Evita `!!` nel codice di produzione e astrazioni non necessarie.
- Non modificare struttura dei moduli o versioni delle dipendenze senza necessità
  legata al task. Aggiorna il README quando cambi percorsi o comandi.

## Presentation MVI

- I ViewModel con effetti estendono `BaseEffectViewModel<Intent, State, Effect>`.
  Per schermate prive di effetti usa `BaseViewModel` quando appropriato.
- Implementa `onAction`; la UI invia intent tramite `dispatch`.
- Usa `state`, `updateState` ed `emitEffect` della base; non duplicare StateFlow o canali.
- Lo stato immutabile è l'unica fonte di verità per lista, form, caricamento e operazioni.
- I composable renderizzano lo stato e inviano intent; non eseguono logica di persistenza.
- Raccogli lo stato rispettando il lifecycle. Consuma il flow `effect` in un solo punto,
  quando la schermata è attiva; browser e snackbar appartengono alla route/UI.
- Mantieni il blocco dei tap duplicati durante salvataggio/eliminazione, gli errori
  recuperabili e il retry senza perdere gli input dell'utente.

## Coroutines e Koin

- Gli intent usano `Dispatchers.Main.immediate`; le query DB usano il dispatcher
  iniettato nel datasource. Non spostare indiscriminatamente la logica UI su IO.
- Usa structured concurrency e `viewModelScope`; non usare `GlobalScope`.
- Nei catch rilancia `CancellationException`; non trasformare la cancellazione in errore UI.
- Aggiorna lo stato atomicamente con `updateState`, senza effetti collaterali nella lambda.
- Usa constructor injection. Domain e ViewModel non devono chiamare `get()` o `by inject()`.
- Registra data/domain nei moduli Koin condivisi e ViewModel/driver Android nei moduli dell'app.
- Recupera i ViewModel con `koinViewModel()`, mantenendo il lifecycle del ViewModelStore.
- Nei test DI usa `koinApplication` isolato, chiudi il container e non usare il contesto globale.

## Database e compatibilità

- Mantieni `applicationId = "it.linkspockets"` e il file `links.db`, salvo richiesta esplicita.
- Non cancellare database o dati dell'utente per risolvere problemi di schema.
- Per evoluzioni dello schema considera migrazioni SQLDelight `.sqm` per i dati esistenti;
  non affidarti alla sola modifica del `CREATE TABLE`.
- Non introdurre backend, sincronizzazione o UI iOS senza richiesta.

## Verifiche

Usa il wrapper Gradle dalla root del repository e JDK **17**.
Su macOS, se necessario, configura l'ambiente della shell:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

Comandi disponibili:

```bash
# Build dell'app Android:
./gradlew :androidMain:assembleDebug
# Test mirati del ViewModel MVI:
./gradlew :androidMain:testDebugUnitTest --tests 'it.linkspockets.presentation.link.LinkViewModelTest'
# Test del wiring Koin Android:
./gradlew :androidMain:testDebugUnitTest --tests 'it.linkspockets.di.KoinModulesTest'
# Test domain e SQLite della libreria condivisa:
./gradlew :composeApp:jvmTest
# Controllo whitespace sulle modifiche tracciate:
git diff --check
```

- Aggiungi o aggiorna test per il comportamento modificato; esegui prima quelli mirati.
- Usa dispatcher virtuali e repository fake per i test MVI; SQLite reale per la persistenza.
- Verifica che i test selezionati vengano effettivamente eseguiti, non solo che Gradle termini.
- Non avviare emulatori o installare l'app senza richiesta.
- Riporta comandi, risultati e limiti delle verifiche; non dichiarare test mai eseguiti.

## Salvaguardie

- Ispeziona `git status` prima delle modifiche e preserva il lavoro dell'utente,
  incluse le modifiche in staging. Non riordinare lo staging senza richiesta.
- Preferisci modifiche minime; evita refactoring o formattazioni estranei al task.
- Non fare commit, push, deploy, reset o pulizie distruttive senza richiesta esplicita.
- Non modificare file generati, database, credenziali o `local.properties` senza necessità.
- Se la richiesta comporta un cambiamento architetturale ambiguo o perdita di dati,
  chiarisci il punto prima di procedere.

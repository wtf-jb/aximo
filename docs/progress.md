# Fortschritt

## Erledigt
- Requirements, Design-System, Mockups (siehe `docs/`)
- Umgebung eingerichtet (04.10.): `dl.google.com` freigegeben, Android SDK unter `/opt/android-sdk`; lokal laufen `./gradlew test assembleDebug lintDebug`. Room-Schema v1 in `data/schemas` committet
- Schritt 1 Projekt-Setup (`docs/prompts/01-setup.md`), PR #1 (CI grün, gemergt):
  - Gradle-Projekt `:app`, `:data`, `:domain`, Version Catalog, Wrapper 9.8.0
  - Schriften als Variable Fonts + OFL-Lizenzen (`licenses/`)
  - Theme nach `compose.md`/`tokens.json`: Light/Dark, ExtendedColors, Typografie mit tnum, Shapes, Spacing, Sizes; Theme-Wahl System/Hell/Dunkel (DataStore)
  - App-Hülle mit unterer Navigation (Heute, Pläne, Statistik, Coach) und Platzhaltern, Strings EN/DE, keine INTERNET-Permission
  - Theme-Showcase (nur Debug, Button auf „Heute“)
  - Platzhalter-Test in `:domain` (`ThemeModeTest`)
  - CI: `.github/workflows/android.yml` (Test + assembleDebug, Artefakt `app-debug`, Pre-Releases `debug-latest` / `pr-N`)
- Schritt 2 Domain-Modell und Room-Datenbank, PR #2 (CI grün, gemergt):
  - Domain-Modelle für alle Prio-A-Entitäten aus requirements.md Abschnitt 6 mit Validierung, `WeightUnit` (kg/lbs)
  - Repository-Interfaces `ExerciseRepository`, `RoutineRepository` in `:domain`
  - Room-DB `AximoDatabase` v1 (alle Prio-A-Tabellen), DAOs, Mapper, Room-Repositories, Koin-Verdrahtung
  - Tests: Domain (Validierung, Einheiten), Room via Robolectric (Übungen, Routinen, Workouts/Kaskaden)
- Schritt 3 Übungsverwaltung (A-01), PR #3 (CI grün, gemergt):
  - Domain: `BodyRegion`, `ExerciseFilter` (Suche ohne Umlaut-/Groß-Klein-Beachtung, Region), `ExerciseDraft` (Formular + Validierung, kg/lbs)
  - UI: Übungsliste (Suche, Region-Chips, Archiv, leerer Zustand), Formular Neu/Bearbeiten/Archivieren; Einstieg über „Pläne“ → „Übungen“
  - Komponenten: `ChoiceChip`, `SearchField`, `LabeledTextField`, `CircleIconButton`, `PillTextButton`
  - Tests: Domain (Filter, Formular), ViewModels mit Fake-Repository
- Schritt 4 Workout-Logging (A-02), PR #4 (gemergt):
  - Domain: `WorkoutLogic` (Vorbelegung, nächster Satz, aktiver Satz inkl. Supersatz-Runden, Gruppierung, Nummerierung, letzte Leistung), `WorkoutRepository`, `TimeSource`
  - Data: Relationen Workout → Übungen → Sätze, laufendes Workout als Flow, letzte Session pro Übung
  - UI: Workout-Screen mit Uhr, SetRow (erledigt/aktiv/offen), Werte-Dialog mit −/+, Satztyp-Menü, Notizen, Beenden/Verwerfen; Auswahlmodus der Übungsliste mit „Als Supersatz“
  - Tests: Domain, Room, ViewModels; Lint in CI
- Schritt 5 Rest-Timer (A-03), PR #5 (gemergt):
  - Domain: `RestTimer` (Restzeit aufgerundet, Fortschritt, +15 s), `RestTimerLogic` (Start nach Satz, Supersatz erst nach der Runde mit längster Pause, nächster Satz), `RestTimerController` (Zustand, Ablauf, Effekte als Interface)
  - App: `RestTimerService` (Foreground `specialUse`, Wakelock während der Pause), `RestNotifications` (laufend mit Countdown, „+15 s“, „Überspringen“; „Pause vorbei“ mit Ton + Vibration, Fallback-Vibration ohne Notification-Recht)
  - UI: `RestTimerBar` schwebend im Workout-Screen; Notification-Berechtigung beim Öffnen des Workouts
  - Tests: Domain (Timer, Logik, Controller mit virtueller Zeit), ViewModel

- Schritt 6 Routinen und Pläne (A-05), PR #6 (gemergt):
  - Domain: `RoutineLogic` (nächste Routine reihum, Sätze aus Zielen + letzter Session), `RoutineDraft` (Bearbeiten, Reihenfolge, Supersätze aufräumen), `WorkoutStarter` (frei oder aus Routine, nie zwei laufende)
  - Data: Routinen mit Übungen als Flow, letzte Einheit pro Routine
  - UI: Pläne-Tab (Freies Training, Übungen, Routinenliste mit „Als Nächstes“ und Start-Button), Routine bearbeiten (Name, Ziele-Dialog, Menü mit Verschieben/Supersatz lösen/Entfernen, Übung hinzufügen, Löschen), Workout zeigt Routinenname und Ziele, Heute „Push A starten“
  - Tests: Domain, Room, ViewModels

- Schritt 7 Progression (A-06) und Workout-Abschluss, PR #7 (gemergt):
  - Domain: `ProgressionRules` (Double Progression, Reduktion nach 2× unter Untergrenze, Bodyweight Wdh. → Gewicht, Rundung auf Scheibenschritte), `Records` (e1RM Epley ≤ 12 Wdh., Volumen, neue Bestwerte), `WorkoutFinisher`
  - UI: Hinweis-Chip „Progression“ im Workout; Abschluss-Screen mit Bilanz, Bestwerten, „Fürs nächste Mal“, Bewertung, Notiz

- Schritt 8 Cardio manuell (A-04), PR #8 (gemergt):
  - Domain: `CardioMath`, `CardioActivities`, `CardioDraft`, `CardioRepository`, `RecentActivity`
  - UI: „Cardio erfassen“ nach `Cardio.html`, Bearbeiten/Löschen; Pläne-Tab mit „Cardio erfassen“; „Zuletzt“ auf Heute
- Schritt 9 Auswertungen (A-07), PR #9:
  - Domain `stats`: `StatsPeriod`, `ExerciseStats` (Sessions, e1RM- bzw. Wdh.-Verlauf, Bestwerte, Wdh. bei Gewicht, PR-Sessions), `MuscleVolume` (Sätze je Region pro Woche), `Consistency` (Heatmap 12 Wochen, Einheiten/Woche, Streak), `CardioTrends`, `WorkoutComparison`
  - Data: alle beendeten Workouts / Cardio-Einträge als Flow, vorherige Einheit einer Routine
  - UI: Statistik-Tab mit Vico-Diagramm, Übungsdetail (Verlauf, Diagramm, Bestwerte, Info, „Nächstes Mal“), Workout-Detail aus „Zuletzt“ (Abschluss-Screen), Volumen-Vergleich ggü. letzter gleicher Routine
  - Tests: Domain, Room, ViewModels

- Schritt 10a Einstellungen (A-09), PR #10 (gemergt):
  - Domain: `TrainingSettings` (Einheit, Standard-Pause, `WeightSteps` Langhantel/Kurzhantel), Einheitenwechsel nimmt Standardschritte mit; `ExerciseDraft.new`/`withEquipment`; `WorkoutFinisher` rundet auf die Schritte aus den Einstellungen
  - Data: Trainings-Einstellungen im DataStore
  - UI: Einstellungen nach `Einstellungen.html` (Sprache ab Android 13, Gewichtseinheit, Darstellung, Standard-Pause, Gewichtsschritte), Einstieg über das Regler-Icon auf „Heute“; Einheit app-weit über `LocalWeightUnit`
  - Tests: Domain, DataStore, ViewModels

- Schritt 10b Export/Import (A-08), PR #11 (gemergt):
  - Domain: `BackupRepository`, `BackupException` (ungültig / neuere Version), `SetsCsv` (eine Zeile pro Satz)
  - Data: `BackupFile` (JSON, `schemaVersion` 1, Tabellen 1:1 mit IDs, Trainings-Einstellungen), `BackupDao`, `RoomBackupRepository` (Export in einer Transaktion; Restore prüft Domain-Regeln und ersetzt alles in einer Transaktion, bei Fehler bleibt alles wie es war)
  - UI: „Daten & Backup“ in den Einstellungen: Export JSON, Export CSV, Import mit Bestätigung, über Androids Dateidialog
  - Tests: Domain, Room (Rundreise, Ersetzen, kaputte Dateien, neuere Version), ViewModel

- Schritt 11 „Heute“ vervollständigen, PR #12 (gemergt):
  - Domain: `NextWorkout` (Dauer-Schätzung, Progressions-Vorschau), `WeekBar` (Mo–So, Einheiten der Woche)
  - UI: Datum über dem Titel, Hero-Karte „Nächstes Workout“ (Routine, Übungen, ca. Dauer, bis zu 3 Änderungen aus der Progression, „Workout starten“), Karte „Diese Woche“
  - Coach-Tab aus der Navigation entfernt (Prio B, war nur Platzhalter)
  - Tests: Domain, ViewModel

- Schritt 12 RPE als Alternative zu RIR (A-02), PR #13 (gemergt). **Prio A damit komplett.**
  - Domain: `SetRating` (RIR/RPE) in `TrainingSettings`, `Effort` (Umrechnung RIR = ⌊10 − RPE⌋, RPE-Eingabe in halben Punkten, nur eine Skala pro Satz gespeichert); Progression liest RPE als RIR
  - Data: Einstellung im DataStore und im Backup (`setRating`, ältere Dateien = RIR)
  - UI: Einstellung „Satzbewertung“; Workout-Spalte, Werte-Dialog, Satz-Chips und Routinen-Ziele zeigen die gewählte Skala (die andere wird umgerechnet)
  - Tests: Domain, Data, ViewModels

- Feinschliff 1: Übungskatalog, PR #14 (gemergt):
  - Domain: `CatalogExercise` (38 gängige Kraft-/Bodyweight-Übungen mit Muskelgruppen, Equipment, Wdh.-Bereich), `ExerciseCatalog`, `CatalogSeeder` (beim ersten Start, nur ohne Kraft-/Bodyweight-Übungen; „fehlende ergänzen“ nach `catalogId`)
  - App: Namen DE/EN als Strings, Anlage beim App-Start; Einstellungen „Standard-Übungen ergänzen“
  - Tests: Domain, Seeder, ViewModel
- Feinschliff 2: Wochenziel, PR #14 (gemergt):
  - `TrainingSettings.weeklyGoal` (kein Ziel oder 1–7 Einheiten), DataStore und Backup
  - „Heute“: „3 von 4 Einheiten“; Einstellungen: Zeile „Wochenziel“

- Feinschliff 3: Drag-and-drop in Routinen, PR #15 (gemergt):
  - Domain: `RoutineDraft.groups()`/`moveGroup()` (Karten verschieben, Supersatz als Block)
  - UI: Karte lange drücken und ziehen; Haptik, Schatten, Nachbarn rutschen animiert; ⋯-Menü „Nach oben/unten“ bleibt (Barrierefreiheit)

## Prio B (freigegeben 04.10., inkl. `INTERNET`-Permission)
Plan: B-01 → B-02 → … → B-07 in kleinen PRs. Coach-Tab kommt zurück in die Navigation, sobald er Inhalt hat (B-02).

## In Arbeit
- B-01 KI-Provider-Layer:
  - Domain `ai`: `AiProvider`, `AiRequest`/`AiMessage`, `AiException` (Gründe), `AiProviderProfile` + `AiProfiles` (URL-Prüfung, aktives Profil), `AiProfileDraft` (Formular, Key-Pflicht nur Anthropic), `ApiKeyChange`, `AiProfileRepository`, `AiConnectionTester`
  - Modul `:ai` (reines Kotlin, Ktor): `OpenAiCompatibleProvider` (`/chat/completions`), `AnthropicProvider` (`/v1/messages`), `KtorAiProviderFactory`, `AiHttp` (Timeouts, Fehler-Mapping, Key-Schwärzung)
  - Data: Tabelle `ai_profiles` (DB v2, Auto-Migration), `KeystoreAiKeyStore` (AES-GCM, Keystore), `RoomAiProfileRepository`
  - App: `INTERNET`-Permission, Klartext erlaubt (Ollama im LAN), OkHttp-Engine; Einstellungen „KI-Coach“ → Profilliste → Formular mit „Verbindung testen“, Löschen mit Bestätigung
  - Tests: Adapter gegen Ktor MockEngine (Request-Format, Statuscodes, Fehlerformen, Timeout, Netz), Domain, Room-Repository, ViewModels

## Nächster Schritt
- B-02 Wöchentlicher KI-Review: `AiSuggestion`-Tabelle, aggregierter Kontext (Volumen, e1RM, Stagnation, Frequenz, RIR-Trend), JSON-Schema + Validierung, Review-Screen nach `Review.html`, Datenschutz-Hinweis vor dem ersten Call, Coach-Tab zurück

## APK aufs Handy
- Stand main: https://github.com/wtf-jb/aximo/releases/download/debug-latest/aximo-debug.apk
- Stand eines PRs N: https://github.com/wtf-jb/aximo/releases/download/pr-N/aximo-debug.apk

## Offen für Jonas
- B-01 auf dem Pixel testen: Profil für Ollama (Unraid/Tailscale) oder einen Cloud-Provider anlegen, „Verbindung testen“
- APK von main installieren und die Liste „Auf dem Pixel prüfen“ aus PR #1 durchgehen; Probleme als Kommentar oder in einer Session melden
- Setup-Skript im Environment prüfen: Plattform-Paket heißt `platforms;android-37.0` (in `docs/cloud-environment.md` korrigiert)
- Routine im claude.ai-UI anlegen (meine per Tool angelegte Routine hat kein Repo und keinen GitHub-Zugang und ist deaktiviert), Prompt siehe `docs/routine-prompt.md`
- Lizenz: Arbeitsannahme GPLv3 (siehe `docs/decisions.md`)

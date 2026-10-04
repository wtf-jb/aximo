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

- B-01 KI-Provider-Layer, PR #16 (gemergt):
  - Domain `ai`: `AiProvider`, `AiRequest`/`AiMessage`, `AiException` (Gründe), `AiProviderProfile` + `AiProfiles` (URL-Prüfung, aktives Profil), `AiProfileDraft` (Formular, Key-Pflicht nur Anthropic), `ApiKeyChange`, `AiProfileRepository`, `AiConnectionTester`
  - Modul `:ai` (reines Kotlin, Ktor): `OpenAiCompatibleProvider` (`/chat/completions`), `AnthropicProvider` (`/v1/messages`), `KtorAiProviderFactory`, `AiHttp` (Timeouts, Fehler-Mapping, Key-Schwärzung)
  - Data: Tabelle `ai_profiles` (DB v2, Auto-Migration), `KeystoreAiKeyStore` (AES-GCM, Keystore), `RoomAiProfileRepository`
  - App: `INTERNET`-Permission, Klartext erlaubt (Ollama im LAN), OkHttp-Engine; Einstellungen „KI-Coach“ → Profilliste → Formular mit „Verbindung testen“, Löschen mit Bestätigung
  - Tests: Adapter gegen Ktor MockEngine (Request-Format, Statuscodes, Fehlerformen, Timeout, Netz), Domain, Room-Repository, ViewModels

- B-02a Wochen-Review, Logik und Speicher, PR #17 (gemergt):
  - Domain `review`: `ReviewContext` + `ReviewContextBuilder` (6 Wochen: Einheiten/Woche, Routinen mit Zielen, Trend je Übung mit e1RM/Wdh., „Sessions seit Bestwert“, letzter Top-Satz, Ø-RIR; Volumen je Region; RIR-Trend 1./2. Hälfte; Übungen zum Hinzufügen), `SuggestionChange` (Sätze, Wdh.-Bereich, Ziel-RIR, Übung hinzufügen/entfernen), `AiSuggestion`, `AiReview`, `SuggestionApplier` (prüft gegen aktuelle Routine, wendet an), `ReviewService` (Kontext, Review erstellen, übernehmen, verwerfen), `AiReviewRepository`
  - `:ai`: `ReviewPrompt` (Version `review-v1`, JSON-Schema im Systemprompt, Kontext als JSON), `ReviewParser` (Schema-Prüfung, ungültige Vorschläge werden verworfen und gezählt), `KtorReviewGenerator`
  - Data: Tabellen `ai_reviews`, `ai_suggestions` (DB v3, Auto-Migration), Payload als JSON
  - Tests: Domain, Parser/Prompt, Room, `ReviewService` mit Fakes

- B-02b Coach-Tab mit Wochen-Review (`Review.html`), PR #18 (gemergt):
  - Coach-Tab wieder in der Navigation, aber nur mit KI-Profil (`MainViewModel.aiAvailable`); verschwindet das letzte Profil, springt die App auf Heute
  - `CoachScreen`: KW + Zeitraum, dunkle Zusammenfassungskarte, „N Vorschläge“ + „Alle übernehmen“, Karten mit Kategorie, Routine, Titel, Diff (alt durchgestrichen → neu, NEU/RAUS-Badge), Begründung, Verwerfen/Übernehmen; Status „Übernommen“/„Verworfen“; „Passt nicht mehr zur Routine“, wenn sie inzwischen geändert wurde
  - „Review erstellen“ mit Datenschutz-Hinweis beim ersten Mal (`AiPreferences`, DataStore), Fehleranzeige, „Gesendete Daten ansehen“ (exakt das gesendete JSON)
  - Tests: `CoachViewModel`
- Nicht enthalten: Coach-Chat-Composer (B-05), „Rückgängig“ (siehe decisions.md)

- B-02c Wöchentlicher Review automatisch, PR #19 (gemergt):
  - Domain: `WeeklyReviewSetting` (an/aus, Wochentag, Stunde; Standard aus, So 18:00), `WeeklySchedule.nextRun` (Zeitzone, Sommerzeit), `AiPreferences` erweitert
  - App: `WeeklyReviewWorker` (WorkManager, alle 7 Tage, nur mit Netz und Zustimmung, Wiederholung bei Netz-/Server-Fehlern), `WeeklyReviewScheduler` (hält den Job passend zur Einstellung, ohne Profil aus), Notification „Wochen-Review bereit“
  - Einstellungen KI-Coach: „Wöchentlicher Review“ (Schalter, erstes Einschalten mit Datenschutz-Hinweis), „Zeitpunkt“ (Tag, dann Uhrzeit), „Gesendete Daten ansehen“
  - Heute: Karte „Wochen-Review bereit“ mit Zahl offener Vorschläge, öffnet den Coach-Tab
  - Tests: Zeitplan, DataStore, Settings-ViewModel

- B-03 Plan-Generierung, PR #20 (gemergt):
  - Domain `plan`: `PlanRequest` (Ziel, Tage 2–6, Dauer, Equipment, Einschränkungen als Freitext) + `PlanOptions`, `PlanInput` (nutzbare Übungen: Kraft/Bodyweight, nicht archiviert, passendes Equipment, max. 120), `PlanBuilder` (prüft Antwort gegen angebotene Übungen und Wertebereiche, verwirft und zählt Unpassendes), `PlanProposal` (Entwurf), `PlanService` (erzeugen, speichern ans Listenende)
  - `:ai`: `PlanPrompt` (`plan-v1`, Schema im Systemprompt, nur Wünsche + Übungsliste als JSON, kein Trainingsverlauf), `PlanParser`, `KtorPlanGenerator`; gemeinsame JSON-Helfer in `JsonRead` (auch vom `ReviewParser` genutzt)
  - UI: „Mit KI erstellen“ neben „Übungen“ auf Pläne (nur mit KI-Profil) → Formular (Chips, Freitext) → Entwurf (Zusammenfassung, Routinen mit Übungen und Zielen, je Routine „Entfernen“, „N Routinen speichern“, „Wünsche ändern“); Datenschutz-Hinweis beim ersten KI-Aufruf, „Gesendete Daten ansehen“
  - Tests: Domain (Builder, Eingabe), Parser/Prompt, `PlanService`, `PlanGeneratorViewModel`, `PlansViewModel`

- B-06 Übungskatalog (Text), PR #20 (gemergt):
  - Asset `exercise_catalog.json` (668 Übungen aus free-exercise-db, Unlicense; `tools/build_exercise_catalog.py`, Lizenztext in `licenses/`), Parser `CatalogFile` (`:data`), `AssetCatalogRepository` (`:app`)
  - Domain `catalog`: `CatalogEntry`, `CatalogSearch` (Suche, Region, Übung aus Eintrag, Anleitung zu `catalogId`), Zuordnung der 38 Startübungen zu Katalogeinträgen
  - UI: „Aus Katalog hinzufügen“ in der Übungsliste (auch im Picker), Katalog-Screen mit Suche, Region-Chips, Details mit Anleitung, „Zu meinen Übungen“; Anleitung im Tab „Info“ der Übung; Badge „EIGENE“
  - Tests: Suche/Zuordnung, Parser, ViewModels, Prüfung der echten Asset-Datei
  - Keine Bilder (Entscheidung Jonas 04.10.: weglassen)

- B-04 Freitext-/Sprach-Logging, PR #21 (gemergt):
  - Domain `logging`: `LoggingInput` (Text + Übungsnamen + Einheit/Skala), `ExerciseMatcher` (lokaler Fuzzy-Match: sicher / unsicher mit 2–3 Kandidaten / kein Treffer), `LoggingBuilder` (Wertebereiche, „3x8“ → drei Sätze, Einheit → kg, RIR/RPE, neue Übung mit Katalog-Vorschlag), `LoggingProposal` (Vorschau, Einträge abwählbar, Auswahl), `LoggingService` (parsen, speichern als erledigte Sätze, neue Übungen erst beim Speichern), `SpeechInput` (Interface)
  - `:ai`: `LoggingPrompt` (`logging-v1`, Schema im Systemprompt, Antwortsprache = App-Sprache), `LoggingParser`, `KtorLoggingGenerator`; `JsonRead` um Dezimalzahlen erweitert
  - App: `AndroidSpeechInput` (on-device `SpeechRecognizer`, Mikrofon ausgeblendet ohne on-device-Erkenner; `RECORD_AUDIO` erst beim ersten Tippen), `LoggingScreen` (Textfeld + Mikrofon → Vorschau mit Schaltern/Kandidaten-Chips → „N Sätze speichern“), Button „Per Text oder Sprache erfassen“ im Workout (nur mit KI-Profil)
  - `PlannedSet` um `rpe` und `completedAt` ergänzt (Standard `null`)
  - Tests: Matcher, Builder/Vorschau, Parser/Prompt, `LoggingService`, `LoggingViewModel` (inkl. Sprache mit Fake), `WorkoutViewModel` (Button nur mit Profil)

- B-05 Coach-Chat, PR #22 (gemergt):
  - Domain `chat`: `ChatContext` + `ChatContextBuilder` (Review-Kontext 6 Wochen + Bestwert je Übung in 6 × 4 Wochen, ohne Cardio), `ChatHistory` (letzte 12 Nachrichten, ~12 000 Zeichen, beginnt mit Frage), `ChatMessage`, `ChatPayload`, `ChatService` (senden, prüfen, speichern, „Neues Gespräch“), `AiChatRepository`; `SuggestionApplier.applicable` (auch vom Review genutzt); `AiSuggestion` hat `reviewId` oder `chatMessageId`
  - `:ai`: `ChatPrompt` (`chat-v2`, Kontext im Systemprompt, Verlauf als Nachrichten, frühere Antworten im Antwortformat), `ChatParser` (`reply` + max. 3 Vorschläge, Text ohne JSON = Antwort ohne Vorschläge), `KtorChatGenerator`, `SuggestionJson` (gemeinsam mit `ReviewParser`). Kein Streaming
  - Data: Tabelle `ai_chat_messages`, `ai_suggestions.reviewId` nullable + `chatMessageId` (DB v4, Auto-Migration, Migrationstest), `RoomAiChatRepository`; `chatNoticeAccepted` im DataStore
  - UI: Coach-Tab mit Umschalter „Wochen-Review | Chat“; Chat mit Blasen, Vorschlagskarten inline (Übernehmen/Verwerfen über `ReviewService`), „Coach denkt …“, Beispielfragen, Eingabefeld, Hinweis + „Daten ansehen“, „Neues Gespräch“ mit Rückfrage, eigener Datenschutz-Hinweis beim ersten Senden
  - Tests: Domain (Kontext, Kürzung, `applicable`), Prompt/Parser + MockEngine-Rundreise, Room (Repository, Migration 3→4, DataStore), `ChatService`, `ChatViewModel`
  - Nachtrag (Feedback Jonas): Coach erstellt Pläne direkt aus dem Chat (Plan-Karte mit abwählbaren Routinen, „N Routinen speichern“; fehlende Übungen lokal aus dem Katalog zugeordnet und beim Speichern angelegt) und kann zusätzlich Übungen tauschen, Reihenfolge ändern, Routinen umbenennen und löschen. Prompt `chat-v2`

- KI-Profil: Modellauswahl, PR #23 und #24 (gemergt):
  - Domain: `AiModelLister`, `AiModels.clean`, `AiProfileDraft.canListModels` (gültige URL und Key)
  - `:ai`: `KtorModelLister` (OpenAI-kompatibel `GET {base}/models`, Anthropic `GET /v1/models`), `getJson` in `AiHttp`
  - UI: Formularreihenfolge Base-URL, Key, Modell; Dropdown-Pfeil und automatisches Laden (800 ms Debounce) erst mit URL und Key; Status „N Modelle verfügbar“ / Fehler mit „Erneut laden“; Freitext bleibt möglich
  - Tests: MockEngine, Domain, ViewModel

- Entwickler-Werkzeuge (PR #26, gemergt): Theme-Showcase, „Testdaten laden“ (~3 Jahre), „Alle Daten löschen“ in Einstellungen → Entwickler (nur Debug)
- Kalender (PR #27 + #28, gemergt): Kalender-Icon auf „Heute“ → Monatsansicht (Kraft dunkel, Cardio getönt), Tag antippen → Einträge, Monate per Wisch oder Pfeil; Domain `TrainingCalendar`

## In Arbeit
- Kalorienschätzung (Branch `claude/untitled-session-70idaq`, PR gegen main):
  - Domain: `CalorieEstimate` (netto, konservativ, auf 10 kcal abgerundet; Kraft MET 3,5 × Dauer, Laufen/Gehen pro km, Rad/Rudern nach Tempo, Rest MET 4), `TrainingSettings.bodyWeightKg`
  - Data: Körpergewicht im DataStore und im Backup (`bodyWeightKg`, ältere Dateien = keins)
  - UI: Einstellungen → Allgemein → „Körpergewicht“; Abschluss-Screen Kachel „≈ N kcal“; „Cardio erfassen“ Zeile unter der Pace. Ohne Gewicht Hinweis auf die Einstellungen
  - Tests: Domain, DataStore/Backup, ViewModels (Einstellungen, Abschluss, Cardio)
  - Formeln und Gründe: `docs/decisions.md`

## Nächster Schritt
- B-07 Auto-Backup (Periodischer JSON-Export in frei wählbaren Ordner, Intervall konfigurierbar, Rotation der letzten N Backups; Anforderung siehe `docs/requirements.md`)
- Periodisierung (Rest von B-02: Zyklen/Blöcke mit Deload-Woche, „Block 1, Woche 4“ auf Heute, Deload-Vorschlag im Review): **nach B-07 einplanen**, wenn Jonas ≥ 4–6 Wochen Daten mit der App hat (Review-Fenster = 6 Wochen; Deload-Regeln brauchen echte RIR-/Stagnations-Verläufe) und die ersten Reviews auf dem Pixel getestet sind. Sinnvoller Startpunkt: Montag nach einer Deload-Woche oder direkt nach einem mit B-03 erzeugten Plan (= „Block 1“). Vorher von Jonas festlegen: Blocklänge (z. B. 4 + 1 Deload), Deload-Art (Volumen −40 % oder Gewicht −10 %)

## APK aufs Handy
- Stand main: https://github.com/wtf-jb/aximo/releases/download/debug-latest/aximo-debug.apk
- Stand eines PRs N: https://github.com/wtf-jb/aximo/releases/download/pr-N/aximo-debug.apk

## Offen für Jonas
- Katalog mit Fotos und deutschen Texten testen (APK vom PR): Übungen → „Aus Katalog hinzufügen“ → Namen sind deutsch, Suche nach „Kniebeuge“ und „squat“ findet beide; Eintrag antippen → zwei Fotos (Start/Ende) über der Anleitung; Info-Tab einer Startübung (z. B. Bankdrücken) zeigt Fotos und deutsche Anleitung. Gerätesprache Englisch → englische Texte. Übersetzungsfehler gern als Liste melden
- Notizfeld im Abschluss, Textfeld in „Per Text erfassen“ und Einschränkungen in „Mit KI erstellen“: im hellen Theme jetzt weiß statt unsichtbar
- Kalorien testen (APK vom PR): Abschluss eines Workouts bzw. „Cardio erfassen“ ohne Körpergewicht → Hinweis; Einstellungen → „Körpergewicht“ eintragen (auch in lbs), dann zeigt der Abschluss „≈ N kcal“ und Cardio live die Schätzung (Formeln überarbeitet, brutto nach Compendium: Laufen 10 km in 60 min bei 80 kg ≈ 840 kcal, 60 min Kraft ≈ 420 kcal mit freien Gewichten, ≈ 500 mit Kniebeuge/Kreuzheben, ≈ 290 nur Maschine/Bodyweight). Feld leeren → Schätzung weg. Formeln ggf. in `decisions.md` kommentieren
- Kalender testen (APK von main): „Heute“ → Kalender-Icon; Tage mit Training markiert, Tag antippen, Zeile öffnen, Monate zurückblättern
- Entwickler-Werkzeuge testen (APK von main): Einstellungen ganz unten → „Testdaten laden“ (Rückfrage), danach Statistik, Pläne, Verlauf und Coach durchklicken; „Alle Daten löschen“ leert alles, die Standardübungen sind wieder da; „Theme-Showcase“ öffnet sich dort, auf „Heute“ ist der Button weg
- Modellauswahl im KI-Profil testen (APK von main): Einstellungen → KI-Coach → Profil. Nach Base-URL und Key erscheint „N Modelle verfügbar“, über den Pfeil auswählen; falscher Key → Fehlermeldung + „Erneut laden“; bestehendes Profil öffnen → Liste lädt mit gespeichertem Key; ohne Key keine Liste (auch Ollama im LAN: Modell tippen)
- B-01/B-02 auf dem Pixel testen: Profil für Ollama (Unraid/Tailscale) oder einen Cloud-Provider anlegen, „Verbindung testen“, im Coach-Tab einen Review erstellen
- B-06 auf dem Pixel testen (APK von main): Übungen → „Aus Katalog hinzufügen“, suchen, Details, hinzufügen; Info-Tab einer Startübung zeigt die Anleitung
- B-04 auf dem Pixel testen (APK von main, braucht KI-Profil): Workout starten → „Per Text oder Sprache erfassen“
  - Text „Bankdrücken 3x8 80 kg RIR 2“ → Datenschutz-Hinweis beim ersten Mal, „Daten ansehen“ (nur Text + Übungsnamen), Vorschau: drei Sätze, speichern → erscheinen als erledigte Sätze im Workout
  - „Kniebeuge 100 kg 5 Wdh, 105 kg 3 Wdh“, „Klimmzüge 4x10“, mehrere Übungen in einem Satz, „Aufwärmsatz 40 kg 10“
  - Fuzzy-Match: „Kniebeugen“, „bankdrucken“, Tippfehler → automatisch zugeordnet; mehrdeutiger Name (z. B. „Bankdrücken“ bei mehreren Varianten) → Kandidaten-Chips, Speichern erst nach Auswahl; unbekannte Übung („Cable Pullover“) → „Neu: …“ mit Badge, Eintrag abwählen, nichts wird angelegt
  - Einheit lbs und Skala RPE in den Einstellungen umstellen: „225 lbs 5 @8“
  - Mikrofon: erstes Tippen fragt die Berechtigung; Diktat landet im Feld und ist korrigierbar; Berechtigung ablehnen → Hinweis; Flugmodus (on-device-Erkennung geht ohne Netz, nur die KI-Auswertung nicht); fehlt das deutsche Sprachpaket → Hinweis. Ohne on-device-Erkenner ist das Mikrofon nicht sichtbar
  - Fehler: Netz aus, falscher Key, Text ohne Sätze („Hallo“)
- B-03 auf dem Pixel testen (APK von main): Pläne → „Mit KI erstellen“, Plan erzeugen, Entwurf prüfen, speichern
- B-05 auf dem Pixel testen (APK von main, braucht KI-Profil): Coach-Tab → „Chat“
  - Beispielfrage antippen oder „Warum stagniert mein Bankdrücken?“ → beim ersten Mal Chat-Hinweis (auch wenn der Review-Hinweis schon bestätigt war), „Daten ansehen“ zeigt Kontext + Nachrichten
  - Antwort nennt Zahlen aus deinen Daten; „Coach denkt …“ während der Anfrage
  - „Was soll ich an Push A ändern?“ → Vorschlagskarten in der Antwort; eine übernehmen (Routine prüfen), eine verwerfen; Karte, die nach einer Routinen-Änderung nicht mehr passt, zeigt den Hinweis
  - App schließen und neu öffnen → Verlauf und Status der Karten sind noch da
  - „Neues Gespräch“ → Rückfrage → Verlauf leer, übernommene Änderung bleibt in der Routine
  - Fehler: Flugmodus, falscher Key → Fehlermeldung, Frage steht wieder im Feld, erneut senden
  - Tastatur: Eingabefeld bleibt sichtbar, Verlauf scrollt; Wochen-Review weiterhin über den Umschalter, „Wochen-Review bereit“ auf Heute öffnet den Review
  - Plan: „Erstelle mir einen Plan für 3 Tage, Gym“ → Plan-Karte; eine Routine abschalten, „N Routinen speichern“ → in „Pläne“ hinten angehängt, NEU-Übungen stehen in der Übungsliste (mit Anleitung, wenn aus dem Katalog). Ohne Angaben fragt der Coach nach Tagen/Equipment
  - Anpassungen: „Tausche Bankdrücken gegen Kurzhantel-Bankdrücken“, „Setz Kniebeuge an den Anfang von Push A“, „Benenne Push A in Oberkörper A um“, „Lösch Routine X“ → Karten mit Diff, übernehmen, in „Pläne“ prüfen
- APK von main installieren und die Liste „Auf dem Pixel prüfen“ aus PR #1 durchgehen; Probleme als Kommentar oder in einer Session melden
- Setup-Skript im Environment prüfen: Plattform-Paket heißt `platforms;android-37.0` (in `docs/cloud-environment.md` korrigiert)
- Routine im claude.ai-UI anlegen (meine per Tool angelegte Routine hat kein Repo und keinen GitHub-Zugang und ist deaktiviert), Prompt siehe `docs/routine-prompt.md`
- Lizenz: Arbeitsannahme GPLv3 (siehe `docs/decisions.md`)

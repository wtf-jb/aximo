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

## In Arbeit
- Schritt 5 Rest-Timer (A-03), PR #5:
  - Domain: `RestTimer` (Restzeit aufgerundet, Fortschritt, +15 s), `RestTimerLogic` (Start nach Satz, Supersatz erst nach der Runde mit längster Pause, nächster Satz), `RestTimerController` (Zustand, Ablauf, Effekte als Interface)
  - App: `RestTimerService` (Foreground `specialUse`, Wakelock während der Pause), `RestNotifications` (laufend mit Countdown, „+15 s“, „Überspringen“; „Pause vorbei“ mit Ton + Vibration, Fallback-Vibration ohne Notification-Recht)
  - UI: `RestTimerBar` schwebend im Workout-Screen; Notification-Berechtigung beim Öffnen des Workouts
  - Tests: Domain (Timer, Logik, Controller mit virtueller Zeit), ViewModel

## Nächster Schritt
- 6. Routinen und Pläne (A-05)

## APK aufs Handy
- Stand main: https://github.com/wtf-jb/aximo/releases/download/debug-latest/aximo-debug.apk
- Stand eines PRs N: https://github.com/wtf-jb/aximo/releases/download/pr-N/aximo-debug.apk

## Offen für Jonas
- APK von main installieren und die Liste „Auf dem Pixel prüfen“ aus PR #1 durchgehen; Probleme als Kommentar oder in einer Session melden
- Setup-Skript im Environment prüfen: Plattform-Paket heißt `platforms;android-37.0` (in `docs/cloud-environment.md` korrigiert)
- Routine im claude.ai-UI anlegen (meine per Tool angelegte Routine hat kein Repo und keinen GitHub-Zugang und ist deaktiviert), Prompt siehe `docs/routine-prompt.md`
- Lizenz: Arbeitsannahme GPLv3 (siehe `docs/decisions.md`)

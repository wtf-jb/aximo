# Fortschritt

## Erledigt
- Requirements, Design-System, Mockups (siehe `docs/`)
- Umgebung geprüft (04.10.): Cloud-Session hat JDK 21 und Gradle, aber **kein Android SDK und keinen Zugriff auf dl.google.com/maven.google.com** (siehe „Offen für Jonas“). Builds deshalb vorerst nur über GitHub Actions; `:domain`-Tests laufen lokal mit einem Hilfs-Setup nur aus Maven Central
- Schritt 1 Projekt-Setup (`docs/prompts/01-setup.md`), Branch `claude/untitled-session-70idaq`:
  - Gradle-Projekt `:app`, `:data`, `:domain`, Version Catalog, Wrapper 9.8.0
  - Schriften als Variable Fonts + OFL-Lizenzen (`licenses/`)
  - Theme nach `compose.md`/`tokens.json`: Light/Dark, ExtendedColors, Typografie mit tnum, Shapes, Spacing, Sizes; Theme-Wahl System/Hell/Dunkel (DataStore)
  - App-Hülle mit unterer Navigation (Heute, Pläne, Statistik, Coach) und Platzhaltern, Strings EN/DE, keine INTERNET-Permission
  - Theme-Showcase (nur Debug, Button auf „Heute“)
  - Platzhalter-Test in `:domain` (`ThemeModeTest`)
  - CI: `.github/workflows/android.yml` (Test + assembleDebug, Artefakt `app-debug`, Pre-Releases `debug-latest` / `pr-N`)

## In Arbeit
- PR für Schritt 1: CI grün bekommen

## Nächster Schritt
- 2. Domain-Modell und Room-Datenbank (requirements.md Abschnitt 6), mit Tests

## APK aufs Handy
- Stand main: https://github.com/wtf-jb/aximo/releases/download/debug-latest/aximo-debug.apk
- Stand eines PRs N: https://github.com/wtf-jb/aximo/releases/download/pr-N/aximo-debug.apk

## Offen für Jonas
- Cloud-Environment einrichten (Netzwerk `dl.google.com`, `ANDROID_HOME`, Setup-Skript), siehe `docs/cloud-environment.md`. Bis dahin baut nur CI
- Routine im claude.ai-UI anlegen (meine per Tool angelegte Routine hat kein Repo und keinen GitHub-Zugang und ist deaktiviert), Prompt siehe `docs/routine-prompt.md`
- Lizenz: Arbeitsannahme GPLv3 (siehe `docs/decisions.md`)

# Aximo – Fitnesstracker (Android)

Persönliche Trainings-App als Ersatz für FitNotes: Kraft, Bodyweight und Cardio loggen, auswerten, Pläne regelbasiert und später per KI anpassen. Ein Nutzer, später ggf. Open Source auf F-Droid.

## Wo was steht

- `docs/requirements.md`: Anforderungen mit IDs (A-01 … C-03), Datenmodell, Architektur. **Verbindlich.**
- `docs/design/design-system.md`: Prinzipien, Farbrollen, Typografie, Komponentenregeln.
- `docs/design/tokens.json`: alle Design-Werte (Light + Dark).
- `docs/design/compose.md`: Mapping der Tokens auf das Material-3-Theme, Kotlin-Snippets.
- `docs/design/screens.md` + `docs/design/mockups/*.html`: visuelle Referenz pro Screen.

Vor Arbeit an einem Feature die passende Requirement-ID und den Screen lesen.

## Über mich

Ich kann kein Kotlin und kenne Android nur als Nutzer. Ich bin Linux-/Docker-erfahren. Erkläre Entscheidungen kurz, halte den Code einfach und lesbar, keine cleveren Abkürzungen. Kommunikation auf Deutsch, Du, knapp. Code, Bezeichner und Commit-Messages auf Englisch.

## Stack (entschieden, nicht ändern ohne Rückfrage)

- Kotlin, Jetpack Compose, Material 3; MVVM mit StateFlow, unidirektionaler Datenfluss
- Module: `:domain` (reines Kotlin, keine Android-Imports), `:data` (Room, Repositories, Export), `:ai` (Prio B, Ktor), `:health` (Prio C), `:app` (UI, DI, Navigation)
- Room, Koin, Navigation Compose, kotlinx.serialization, kotlinx-datetime, DataStore, Vico, WorkManager
- Gradle Kotlin DSL mit Version Catalog (`gradle/libs.versions.toml`), Gradle Wrapper
- minSdk 28, Zielgerät Pixel 9 Pro
- Jeweils aktuelle stabile Versionen verwenden; im Zweifel prüfen statt raten.

## Harte Regeln

- **F-Droid-tauglich:** keine Google Play Services, kein Firebase, keine Crashlytics/Analytics, keine proprietären SDKs, keine Downloadable Fonts. Schriften (Bricolage Grotesque, Manrope, OFL) liegen als TTF in `res/font`. Icons: Lucide als ImageVector.
- **Offline first:** alle Prio-A-Funktionen ohne Netz. Keine `INTERNET`-Permission, bis Prio B beginnt.
- **Daten nur lokal.** Gewichte intern immer in kg (`Double`), Umrechnung nur in der Anzeige.
- **KI ändert nie direkt Daten.** Jede Änderung ist ein Vorschlag (`AiSuggestion`), angewendet erst nach Bestätigung.
- **Design nur über Tokens.** Keine Hex-Werte, dp-Zahlen oder Schriftgrößen direkt in Composables; immer `MaterialTheme`/`ExtendedColors`/`Spacing`/`Sizes`. Kein Dynamic Color.
- **i18n:** keine hartcodierten UI-Strings; `strings.xml` in `values` (EN) und `values-de` (DE).
- **Tests:** Domain-Logik (Progression, e1RM, Volumen) vollständig unit-getestet, bevor UI darauf aufbaut.

## Prio-Reihenfolge

A (Grundfunktion) → B (KI & Komfort) → C (Health Connect). Nichts aus B oder C bauen, solange A nicht steht. Elemente späterer Prios werden weggelassen, nicht ausgegraut.

## Umgebung

- Entwicklung meist in **Claude Code Cloud-Sessions** (claude.ai/code): Ubuntu, JDK 21, Android SDK unter `$ANDROID_HOME` (über das Setup-Skript des Environments). Kein Gerät, kein Emulator.
- Manchmal lokal auf meinem Linux-Laptop; dann zusätzlich `./gradlew installDebug` aufs Pixel (Wireless Debugging).
- Fehlt im Cloud-Environment etwas (SDK-Paket, Domain), nicht herumbasteln, sondern mir sagen, was ich im Environment ergänzen soll.

## Arbeitsweise: autonom

Ich bin meist nur am Handy und will, dass du selbstständig arbeitest. Also:

- **Nicht auf mein OK warten.** Plan kurz in `docs/progress.md` festhalten, dann umsetzen.
- **Entscheidungen selbst treffen**, wenn es eine vernünftige Standardwahl gibt. Jede Entscheidung mit Begründung in `docs/decisions.md` (Datum, Frage, Entscheidung, Alternativen). Ich lese das und widerspreche, falls nötig.
- **Nur anhalten bei:** Dingen, die nur ich tun kann (Environment-Einstellungen, Zugänge, Konten), Änderungen am entschiedenen Stack oder an den harten Regeln, und allem, was nicht rückgängig zu machen ist.
- Kleine Inkremente in der Reihenfolge aus `docs/prompts/01-setup.md`; jedes endet mit grünem Build (`./gradlew assembleDebug`) und grünen Tests (`./gradlew test`, später Lint).
- Pro Inkrement ein Branch und ein Pull Request mit Zusammenfassung und „Auf dem Pixel prüfen“-Liste.
- Die Debug-APK baut GitHub Actions bei jedem Push und PR (Artefakt `app-debug`). So teste ich auf dem Pixel.
- `docs/progress.md` immer aktuell halten: erledigt, in Arbeit, nächster Schritt, offene Punkte für mich. Eine neue Session muss allein daraus weitermachen können.
- Offene Punkte aus `docs/requirements.md` Abschnitt 7 mit Arbeitsannahme entscheiden und in `docs/decisions.md` vermerken (z. B. App-Name als Arbeitstitel).

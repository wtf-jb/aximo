# Prompt 1: Projekt-Setup (Claude Code Cloud)

Wird vom Startprompt referenziert; Claude Code arbeitet die Schritte autonom ab.

---

Lies CLAUDE.md und alles unter docs/ (die Mockups unter docs/design/mockups/ nur überfliegen). Danach richten wir das Projekt ein. Noch keine Features.

Ziel dieses Schritts:

1. Prüfe die Umgebung: java -version, $ANDROID_HOME, sdkmanager, Gradle. Fehlt das Android SDK, besorg es dir mit den Mitteln der Session; geht das nicht, sag mir genau, was ich im Environment ergänzen soll.
2. App-Name ist Aximo. Wähle eine applicationId (Muster `io.github.<github-user>.aximo`, passend zu F-Droid) und vermerke sie in docs/decisions.md.
3. Lege das Gradle-Projekt an: Module :domain (reines Kotlin/JVM), :data und :app; :ai und :health noch nicht. Gradle Kotlin DSL, Version Catalog, Gradle Wrapper, .gitignore für Android. Aktuelle stabile Versionen von AGP, Kotlin, Compose BOM und Libraries; prüf sie, statt zu raten. minSdk 28.
4. Schriften: Bricolage Grotesque und Manrope als Variable Fonts aus dem Repo google/fonts (OFL) nach app/src/main/res/font, Lizenz OFL.txt dazulegen. Font-Gewichte per FontVariation statt der statischen Dateinamen aus docs/design/compose.md.
5. Compose-Theme exakt nach docs/design/compose.md und tokens.json: Light- und Dark-ColorScheme, ExtendedColors, Typography (tnum), Shapes, Spacing, Sizes. Theme-Wahl System/Hell/Dunkel. Kein Dynamic Color.
6. App-Hülle: untere Navigation Heute, Pläne, Statistik, Coach; je ein Platzhalter-Screen mit Titel im display-l-Stil. Strings in values (EN) und values-de (DE). Keine INTERNET-Permission.
7. Ein Theme-Showcase-Screen, erreichbar über einen Debug-Eintrag, der Farben, alle Textstile, Primär-, Inverse- und Sekundär-Button sowie einen Hinweis-Chip zeigt. So prüfe ich das Theme auf dem Handy in Hell und Dunkel.
8. Ein Platzhalter-Unit-Test in :domain, damit `./gradlew test` etwas ausführt.
9. GitHub-Actions-Workflow: bei jedem Push und Pull Request `./gradlew test assembleDebug`, APK als Artefakt `app-debug` hochladen. Gradle-Cache nutzen.
10. Sag mir am Ende, welche SDK-Pakete (platforms, build-tools) ich fest ins Setup-Skript des Cloud-Environments aufnehmen soll, damit sie gecacht werden; gib mir die fertigen sdkmanager-Zeilen.

Plan kurz in docs/progress.md, dann direkt umsetzen, auf einem Branch arbeiten. Am Ende: Build und Tests grün, Pull Request erstellt, kurze Liste, was ich auf dem Pixel prüfen soll, docs/progress.md aktualisiert.

---

# Danach (Reihenfolge Prio A)

2. Domain-Modell und Room-Datenbank (Abschnitt 6), mit Tests
3. Übungsverwaltung (A-01) und Übungsauswahl-Screen
4. Workout-Logging inkl. SetRow, Satztypen, Supersätze (A-02)
5. Rest-Timer mit Notification (A-03)
6. Routinen und Pläne (A-05)
7. Regelbasierte Progression (A-06), Workout-Abschluss
8. Cardio manuell (A-04)
9. Auswertungen (A-07), Übungsdetail
10. Export/Import (A-08), Einstellungen (A-09)

Weitermachen in einer neuen Session: „Lies CLAUDE.md und docs/progress.md und mach mit dem nächsten Schritt weiter.“

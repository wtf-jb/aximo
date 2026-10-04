# Entscheidungen

| Datum | Frage | Entscheidung | Begründung / Alternativen |
| --- | --- | --- | --- |
| 2026-10-03 | Stack | Kotlin + Jetpack Compose, KMP-ready | Keine Kotlin-Kenntnisse, iOS nicht geplant; Domain Android-frei für spätere KMP-Migration |
| 2026-10-04 | Design | Eigenes Design-System, Light zuerst, Akzent Koralle, kein Dynamic Color | Siehe `docs/design/` |
| 2026-10-04 | Schriften | Gebündelt als TTF statt Downloadable Fonts | Downloadable Fonts brauchen Play Services, bricht F-Droid |
| 2026-10-04 | App-Name | **Aximo** (Repo `aximo`) | Von Jonas gewählt. Verworfen: Ferrum (Namenskollision mit Studio-Apps), Gradus, Robur |
| 2026-10-04 | applicationId | `io.github.wtfjb.aximo`, Debug-Build mit Suffix `.debug` | Muster `io.github.<user>` für F-Droid; Bindestrich aus `wtf-jb` ist in Paketnamen nicht erlaubt. Alternative `io.github.wtf_jb.aximo` (hässlicher). Debug-Suffix erlaubt später Debug und Release parallel auf dem Pixel |
| 2026-10-04 | Build-Versionen | AGP 9.4.0 (eingebautes Kotlin), Gradle 9.8.0, Kotlin 2.4.20, compileSdk/targetSdk 37, Compose BOM 2026.09.00, Koin 4.2.2 | Jeweils aktuelle stabile Version (Stand heute). AndroidX verlangt compileSdk ≥ 37 |
| 2026-10-04 | Schriften | Variable Fonts `bricolage_grotesque.ttf`, `manrope.ttf` (google/fonts, Commit 9710da1); Gewicht per `FontVariation`; bei Bricolage zusätzlich Achse `opsz` = Schriftgröße | Eine Datei pro Familie statt sieben statischer. `opsz` setzen Browser automatisch, so sieht es aus wie in den Mockups. Lizenzen in `licenses/`, weil `res/font` nur Schriftdateien erlaubt |
| 2026-10-04 | Debug-Signatur | Fester Debug-Keystore `app/debug.keystore` im Repo | Jede CI-Runner-Maschine hätte sonst einen eigenen Schlüssel, Updates auf dem Pixel scheitern dann („Paketkonflikt“). Debug-Schlüssel sind nicht geheim. Release-Schlüssel kommt nie ins Repo |
| 2026-10-04 | APK-Verteilung | CI veröffentlicht die APK zusätzlich als Pre-Release: `debug-latest` (main) und `pr-N` (je PR, wird beim Schließen gelöscht) | Artefakte brauchen GitHub-Login und kommen als ZIP; Release-Assets sind direkte Links fürs Handy. Alternative: nur Artefakt |
| 2026-10-04 | Theme-Einstellung | `ThemeMode` + `SettingsRepository` in `:domain`, DataStore-Implementierung in `:data`, DI über Koin. Umschalter vorerst im Theme-Showcase | Schon jetzt die echte Architektur statt Provisorium; der Umschalter wandert mit A-09 in die Einstellungen |
| 2026-10-04 | Icons | Pfade aus den Mockups (Lucide-Stil) als `ImageVector` in `AppIcons`, keine Icon-Library | Nur eine Handvoll Icons nötig; keine Abhängigkeit. Weitere Lucide-Icons (ISC) kommen genauso dazu |
| 2026-10-04 | Backup | Android-Cloud-Backup ausgeschlossen (`backup_rules`, `data_extraction_rules`) | „Daten nur lokal“. Sicherung läuft über den eigenen Export (A-08) |
| 2026-10-04 | Lizenz (offener Punkt 7) | Arbeitsannahme **GPLv3**, LICENSE-Datei erst vor Veröffentlichung | Copyleft passt zu F-Droid und verhindert geschlossene Forks. Alternative Apache 2.0. Jonas entscheidet vor dem Release |
| 2026-10-04 | Branches | Branch-Name gibt die Cloud-Session vor (`claude/…`), ein PR pro Inkrement | Die Session darf nur auf ihren zugewiesenen Branch pushen |
| 2026-10-04 | Merge-Politik | Claude mergt eigene PRs selbst (Squash), sobald CI grün ist | Von Jonas so gewählt. Er testet über `debug-latest`, Probleme behebt das nächste Inkrement |
| 2026-10-04 | Routine | 2× täglich (9:50 und 18:50, Europe/Berlin) startet eine neue Session mit „nächstes Inkrement“ | Von Jonas so gewählt. Muss im claude.ai-UI mit Repo angelegt werden, siehe progress.md |

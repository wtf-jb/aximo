# Fitnesstracker-App – Requirements & Architektur

Stand: 04.10.2026 · Status: Refinement und Design abgeschlossen, bereit für Umsetzung

## 1. Vision

Android-App zum Tracken von Kraft-, Bodyweight- und Cardio-Training als Ersatz für FitNotes. Die App funktioniert vollständig offline und lokal. KI unterstützt bei Planerstellung, Plananpassung und Auswertung, ändert aber nie selbst Daten, sondern macht nur Vorschläge, die der Nutzer bestätigt.

**Probleme mit FitNotes:** keine KI bzw. Plananpassung, veraltete UI/UX, schwache Auswertungen.

**Zielgruppe:** zunächst ein Nutzer (Eigenbedarf). Spätere Veröffentlichung als Open Source möglich.

**Nicht-Ziele (vorerst):** iOS, Cloud-Sync, eigenes Backend, Multi-User, Import der FitNotes-Historie, Social Features.

## 2. Priorisierung

| Prio | Inhalt | Prinzip |
|---|---|---|
| A | Grundfunktion | Offline, lokal, ohne KI vollständig nutzbar |
| B | KI & Komfort | Optional, Key selbst mitbringen, nur Vorschläge |
| C | Health-Daten | Optional, ausschließlich über Health Connect |

Jede Stufe muss ohne die nächste lauffähig sein.

## 3. Funktionale Anforderungen

### Prio A – Grundfunktion

**A-01 Übungsverwaltung**
- Eigene Übungen anlegen, bearbeiten, archivieren
- Felder: Name, Typ (Kraft / Bodyweight / Cardio), primäre und sekundäre Muskelgruppen, Equipment, Notiz
- Pro Übung konfigurierbar: Gewichtsinkrement und Wiederholungsbereich (für die Progression)

**A-02 Workout-Logging (Kraft/Bodyweight)**
- Satz erfassen: Gewicht, Wiederholungen, optional RPE oder RIR
- Satztypen: Warm-up, Arbeitssatz, Drop, Failure
- Supersätze und Zirkel: Übungen gruppieren, Logging springt automatisch zur nächsten Übung der Gruppe
- Bodyweight: Wdh. ohne Gewicht oder mit Zusatz- bzw. Assistenzgewicht
- Letzte Leistung der Übung wird beim Loggen angezeigt
- Notizen pro Übung und pro Workout

**A-03 Rest-Timer**
- Startet automatisch nach einem gespeicherten Satz, Dauer pro Übung konfigurierbar
- Laufende Notification mit Restzeit, Vibration bzw. Ton bei Ablauf
- Bei Supersätzen erst nach dem letzten Satz der Gruppe

**A-04 Cardio (manuell)**
- Felder: Dauer, Distanz, optional Ø-Puls, Höhenmeter, Notiz
- Pace bzw. Geschwindigkeit wird berechnet

**A-05 Routinen & Pläne**
- Routinen anlegen (z. B. Push/Pull/Legs) mit Zielvorgaben je Übung: Satzanzahl, Wdh.-Bereich, Ziel-RIR
- Workout aus Routine starten oder frei trainieren
- Datenmodell unterstützt bereits Zyklen und Blöcke (Mesozyklus, Deload-Woche). Logik dafür folgt in Prio B.

**A-06 Regelbasierte Progression**
- Läuft nach jedem Training lokal, ohne KI
- Standard: Double Progression. Erreichen alle Arbeitssätze die Obergrenze des Wdh.-Bereichs bei RIR ≥ 1, wird beim nächsten Mal das Gewicht um das Inkrement erhöht.
- Liegt eine Übung zweimal in Folge unter der Untergrenze, wird eine Gewichtsreduktion vorgeschlagen.
- Bodyweight: Wdh.-Steigerung, danach Zusatzgewicht
- Rundung auf Scheibenschritte (2,5 kg bzw. 5 lbs, pro Übung überschreibbar)
- Ergebnis erscheint als Vorschlag beim nächsten Workout und ist überschreibbar

**A-07 Auswertungen**
- Progression je Übung: e1RM (Epley, nur Sätze ≤ 12 Wdh.), Bestwerte (Gewicht, Wdh. bei Gewicht, e1RM, Volumen)
- Volumen je Muskelgruppe und Woche: Arbeitssätze, primär zählt 1, sekundär 0,5
- Konsistenz: Trainings pro Woche, Streak, Kalender-Heatmap
- Cardio-Trends: Pace, Distanz, Dauer, Ø-Puls (falls vorhanden)
- Warm-up-Sätze sind aus Volumen und Bestwerten ausgeschlossen
- Zeitraumfilter: 4 Wochen, 3 Monate, 1 Jahr, gesamt

**A-08 Export/Import**
- Vollexport als versioniertes JSON (`schemaVersion`) über das Android-Dateisystem
- Import des eigenen JSON-Formats (Restore)
- Zusätzlich CSV-Export der Sätze für externe Auswertungen

**A-09 Einstellungen**
- Sprache DE/EN (Standard: Systemsprache)
- Einheiten kg/lbs umschaltbar. Intern wird immer in kg gespeichert, umgerechnet wird nur in der Anzeige.
- Standard-Rest-Timer, Standard-Inkremente

### Prio B – KI & Komfort

**B-01 KI-Provider-Layer**
- Provider-agnostisch: OpenAI-kompatibler Endpoint (deckt Ollama, Mistral, OpenRouter u. a. ab) plus nativer Anthropic-Adapter
- Konfigurierbar: Base-URL, Modell und API-Key, mehrere Profile möglich
- Key verschlüsselt über Android Keystore
- Ohne konfigurierten Provider sind die KI-Funktionen ausgeblendet. Die App bleibt voll nutzbar.

**B-02 Wöchentlicher KI-Review**
- Trigger: wöchentlich (Wochentag konfigurierbar) und jederzeit manuell
- Input: aggregierte Trainingsdaten der letzten 4–8 Wochen (Volumen, e1RM-Verlauf, Stagnation, Frequenz, RPE/RIR-Trends). Ab Prio C zusätzlich Health-Daten.
- Output: strukturierte Änderungsvorschläge als JSON-Diff auf den Plan, jeweils mit Begründung
- Deckt Deload-Empfehlungen und Blockwechsel ab (Periodisierung)
- UI zeigt die Vorschläge als Diff, jeder Vorschlag wird einzeln übernommen oder verworfen

**B-03 Plan-Generierung**
- Eingabe: Ziel, Trainingstage pro Woche, Dauer, Equipment, Einschränkungen
- Output: Routine im App-Format als Entwurf, wird vor dem Speichern bestätigt

**B-04 Freitext-/Sprach-Logging**
- Eingabe z. B. „Bankdrücken 3x8 80 kg RIR 2“ per Text oder Sprache (Android SpeechRecognizer, on-device bevorzugt)
- Die KI parst die Eingabe in strukturierte Sätze. Vor dem Speichern erscheint eine Vorschau zur Bestätigung.
- Unbekannte Übungen werden per Fuzzy-Match zugeordnet oder als neue Übung vorgeschlagen

**B-05 Coach-Chat**
- Fragen zur eigenen Historie, z. B. „Warum stagniert mein Bankdrücken?“
- Kontext: aggregierte Daten wie in B-02, keine Rohdaten-Dumps
- Plan-Vorschläge aus dem Chat landen im selben Bestätigungs-Flow wie B-02

**B-06 Übungskatalog**
- Vorbefüllter Katalog mit Anleitung und Bildern, Kandidat: `free-exercise-db` (Public Domain, JSON)
- Bilder werden lokal gebündelt oder optional nachgeladen. Die Größe muss geprüft werden.
- Eigene Übungen bleiben gleichwertig

**B-07 Auto-Backup**
- Periodischer JSON-Export in einen frei wählbaren Ordner (z. B. per Nextcloud-App synchronisiert), Intervall konfigurierbar
- Rotation: die letzten N Backups bleiben erhalten

### Prio C – Health-Daten

**C-01 Health Connect lesen**
- Schlaf, HRV, Ruhepuls, Körpergewicht, Ernährung (Kalorien/Makros)
- Cardio-Sessions inkl. Herzfrequenz importieren, mit Dublettenerkennung gegen manuelle Einträge
- Fließt in den KI-Review (B-02) und den Coach-Chat (B-05) ein

**C-02 Readiness-Check**
- Vor dem Training: Bewertung aus Schlaf, HRV und Ruhepuls
- Bei schlechten Werten Vorschlag, die Intensität zu reduzieren, nur als Hinweis

**C-03 Health Connect schreiben**
- Abgeschlossene Workouts als ExerciseSession schreiben, damit sie in Google Health sichtbar sind

## 4. Nicht-funktionale Anforderungen

| Bereich | Anforderung |
|---|---|
| Offline | Alle Prio-A-Funktionen ohne Netzwerk |
| Datenhaltung | Nur lokal, kein Backend, kein Cloud-Sync |
| Datenschutz | An die KI gehen nur aggregierte Daten. Vor dem ersten KI-Call erscheint ein Hinweis, welche Daten gesendet werden. |
| Sicherheit | API-Keys im Android Keystore, keine Telemetrie |
| i18n | DE/EN von Anfang an, keine hartcodierten Strings |
| Einheiten | kg/lbs, interne Speicherung in kg |
| Logging-UX | Ein Satz mit max. 2 Taps loggbar (Vorbelegung mit letzten Werten) |
| Plattform | Android, minSdk 28. Zielgerät: Pixel 9 Pro |
| Distribution | APK-Sideload, später F-Droid |
| F-Droid-Tauglichkeit | Keine Google Play Services, kein Firebase, keine proprietären SDKs |
| Design | Sehr moderne UI/UX, funktional aber nicht langweilig. Material 3 als Gerüst, eigenes Design-System (`docs/design/`): Light Prio A, Dark Prio B, Akzent Koralle, kein Dynamic Color |

## 5. Architektur & Tech-Stack

**Entscheidung:** Kotlin + Jetpack Compose (nativ), KMP-ready strukturiert.

Begründung: Es gibt keine Kotlin-Vorkenntnisse und die Entwicklung läuft per Vibe Coding, also gewinnt Einfachheit. iOS ist nicht geplant und bräuchte ohnehin einen Mac. Die Domain-Logik bleibt deshalb Android-frei, damit eine spätere Migration auf Kotlin Multiplatform ein Umbau ist und kein Rewrite.

**Module**

| Modul | Inhalt | Android-Abhängigkeit |
|---|---|---|
| `:domain` | Entitäten, Progressionsregeln, e1RM-/Volumen-Berechnung, Use Cases | Nein (reines Kotlin) |
| `:data` | Room-DB, Repositories, Export/Import | Ja |
| `:ai` | Provider-Layer, Prompt-Bau, JSON-Schema-Validierung | Nein (Ktor) |
| `:health` | Health Connect (Prio C) | Ja |
| `:app` | Compose-UI, Navigation, DI, Notifications, WorkManager | Ja |

**Libraries**

| Zweck | Library |
|---|---|
| UI | Jetpack Compose, Material 3 (Expressive) |
| Navigation | Navigation Compose |
| DB | Room (KMP-fähig) |
| DI | Koin |
| HTTP (KI) | Ktor Client |
| Serialisierung | kotlinx.serialization |
| Datum/Zeit | kotlinx-datetime |
| Einstellungen | DataStore |
| Charts | Vico |
| Hintergrundjobs | WorkManager (Auto-Backup, KI-Review-Erinnerung) |
| Health | androidx.health.connect (Prio C) |
| Tests | JUnit, Turbine; Domain-Logik vollständig unit-getestet |

**Architekturmuster:** MVVM mit unidirektionalem Datenfluss (ViewModel → StateFlow → Compose).

**KI-Prinzipien**
- Die KI liefert immer strukturiertes JSON gegen ein definiertes Schema, das validiert wird, bevor es angezeigt wird.
- Die KI schreibt nie direkt in die DB. Jeder Vorschlag wird als `AiSuggestion` gespeichert und nur nach Bestätigung angewendet.
- Prompts sind versioniert und im Code abgelegt.

## 6. Datenmodell (Entwurf)

| Entität | Kernfelder |
|---|---|
| `Exercise` | id, name, type, equipment, incrementKg, repRangeMin/Max, restSeconds, catalogId?, archived |
| `MuscleGroup` / `ExerciseMuscle` | exerciseId, muscleGroupId, role (primär/sekundär) |
| `Routine` | id, name, cycleId? |
| `RoutineExercise` | routineId, exerciseId, order, targetSets, repMin/Max, targetRir, supersetGroup? |
| `Cycle` / `Block` | id, name, start, weeks, isDeload |
| `Workout` | id, start, end, routineId?, note |
| `WorkoutExercise` | workoutId, exerciseId, order, supersetGroup?, note |
| `SetEntry` | workoutExerciseId, order, weightKg, reps, rpe?, rir?, setType, completedAt |
| `CardioEntry` | workoutId?, exerciseId, durationSec, distanceM?, avgHr?, elevationM?, source (manual/healthconnect), externalId? |
| `ProgressionState` | exerciseId, nextWeightKg, nextRepTarget, reason |
| `AiSuggestion` | id, createdAt, type, payloadJson, rationale, status (offen/übernommen/verworfen) |
| `AiProviderProfile` | id, name, kind, baseUrl, model (Key im Keystore) |

## 7. Offene Punkte

- App-Name
- Open-Source-Lizenz (GPLv3 vs. Apache 2.0)
- F-Droid-Kompatibilität der Health Connect-Library prüfen (vor Prio C)
- Bilder zum Übungskatalog: entschieden, weggelassen (ca. 85 MB, siehe `docs/decisions.md`, B-06)
- Feintuning der Progressionsregeln (Isolationsübungen, Bodyweight)
- Prompt-Design und Token-Budget für den KI-Review
- Design-System: Tokens, Komponenten, Schlüssel-Screens

## 8. Nächste Schritte

1. ~~Design-Phase~~ erledigt: Mockups und Design-System, siehe `docs/design/`
2. Projekt-Setup: Gradle-Module, Theme aus Tokens, CI-Build einer APK
3. Umsetzung Prio A in Inkrementen: Datenmodell → Logging → Rest-Timer → Routinen → Progression → Auswertungen → Export

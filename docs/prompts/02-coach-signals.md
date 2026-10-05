# Coach-Signale (Review/Chat verbessern)

Auftrag für eine Umsetzungs-Session. Regeln und Schwellen stehen verbindlich in `docs/decisions.md` (Einträge vom 2026-10-05, „Coach-Signale“ und „Coach-Vorschläge“). **Nicht neu entscheiden, nur umsetzen.** Weicht etwas technisch nicht, dann eine neue Zeile in `decisions.md` mit Begründung.

## Ausgangslage

- `domain/.../review/ReviewContextBuilder.kt` baut `ReviewContext` (`ReviewContext.kt`), pro Übung nur `first/last/best/sessionsSinceBest`.
- `ai/.../ReviewPrompt.kt` (`review-v1`) serialisiert den Kontext und enthält die Regeln; `ChatPrompt` übernimmt dasselbe JSON.
- `ai/.../SuggestionJson.kt` liest/schreibt Vorschläge (Review und Chat), `ReviewParser`/`ChatParser` nutzen es.
- `ReviewService.createReview` und der Chat filtern mit `SuggestionApplier.applicable`.
- Gespeichert in `ai_suggestions` (`data/.../entity/AiEntities.kt`, DB v4, `AximoDatabase.kt`, `MigrationTest.kt`).
- Chip: `SuggestionChange.category()` in `app/.../ui/coach/CoachScreen.kt`.

## Schritte

1. **Domain: Klassifizierung** (`domain/.../review/ExerciseSignals.kt`, reine Funktionen)
   - `enum class TrendStatus { TOO_FEW_DATA, RETURNING, REGRESSING, STAGNATING, PROGRESSING, STABLE }`
   - `enum class EffortStatus { TOO_HARD, ON_TARGET, TOO_EASY }`, `enum class VolumeStatus { BELOW, IN_RANGE, ABOVE }`
   - Funktionen für Status, `newBest`, `sessionsAtRepCeiling`, `rirVsTarget`, Volumenstatus, Pausen, genau nach `decisions.md`. Schwellen als `const val` mit KDoc.
2. **Domain: Kontext erweitern**
   - `ExerciseTrend` um: `recent: List<Double>` (letzte bis 8 Werte, gerundet 0,1), `changePct` (letzter vs. erster, %), `dropFromBestPct`, `daysSinceLast`, `status`, `newBest`, `repMax`/`targetRir` (aus Routine der letzten Einheit, nullable), `sessionsAtRepCeiling`, `recentAvgRir`, `rirVsTarget`, `effort: EffortStatus?`.
   - `RegionVolume` um `status`; `ReviewContext` um `daysSinceLastSession`, `longestBreakDays`.
   - `ReviewContextBuilder` befüllt alles; vorhandene Helfer nutzen (`ExerciseStats`, `ProgressionRules.workingSets`, `Effort.rir`, `MuscleVolume`).
3. **Domain: Plausibilität** (`domain/.../review/SuggestionChecks.kt`)
   - `fun plausible(suggestions, context: ReviewContext, exercises: List<Exercise>): List<GeneratedSuggestion>` nach den drei Regeln in `decisions.md`. Primärregionen über `exercise.primaryMuscles.map { it.region }`.
   - In `ReviewService.createReview` und im Chat-Pfad (`ChatService`) nach `SuggestionApplier.applicable` anwenden; Verworfene zu `dropped` zählen.
4. **Grund (`reason`)**
   - Domain: `enum class SuggestionReason` (Werte aus `decisions.md`), `GeneratedSuggestion.reason` und `AiSuggestion.reason` (nullable, Default null).
   - `SuggestionJson`: optional lesen (unbekannt → null, nie verwerfen) und schreiben; `CHAT_SCHEMA` um `"reason": string` ergänzen.
   - Data: Spalte `reason TEXT` nullable in `AiSuggestionEntity`, DB **v5**, `AutoMigration(4, 5)`, Schema `5.json` exportieren (Build erzeugt es), `MigrationTest` um 4→5 erweitern. Beide Repositories (Review, Chat) speichern/lesen den Grund.
5. **Prompt `review-v2`**
   - `contextJson`: neue Felder in snake_case (`status` lowercase, `recent`, `change_pct`, `drop_from_best_pct`, `days_since_last`, `new_best`, `rep_max`, `target_rir`, `sessions_at_rep_ceiling`, `recent_avg_rir`, `rir_vs_target`, `effort`; Volumen je Region als `{"sets": x, "status": "below"}`; `days_since_last_session`, `longest_break_days`).
   - Regeln ersetzen „Look for stagnation …“ durch: Status/Effort/Volumen-Status **nicht selbst neu bewerten**, sondern übernehmen; Wortwahl passend zum Status (regressing ≠ stagnating); returning → keinen Abbau unterstellen; `sessions_at_rep_ceiling` ≥ 2 → Gewichtserhöhung im Text empfehlen; set_count-Regeln wie in `SuggestionChecks` (damit das Modell sie kennt); jeder Vorschlag mit `reason`; erster Satz des Summary nennt Fortschritt/neue Bestwerte, falls vorhanden.
   - `ChatPrompt`: Regeln für die Statusfelder kurz ergänzen, Version hochzählen.
6. **UI**
   - `category()` nimmt den Grund zuerst: Mapping aus `decisions.md`; neuer String `coach_category_performance` (DE „LEISTUNG“, EN „PERFORMANCE“) und `coach_category_effort` (DE „BELASTUNG“, EN „EFFORT“) in `values` und `values-de`. Ohne Grund → bisheriges Mapping. Keine Layout-Änderung.

## Tests (Pflicht)

- `ExerciseSignalsTest`: jeder Status mit Grenzwerten (genau 5 %, 4,9 %, 10 % bei Wdh., 13 vs. 14 Tage, Reihenfolge der Regeln), `newBest`, Rep-Decke (inkl. RIR 0 bricht ab, RIR nicht geloggt zählt), `rirVsTarget`-Grenzen, Volumenstatus bei 10,0 und 20,0 (IN_RANGE).
- **Regressionsfall aus dem Screenshot:** Bankdrücken e1RM `[110, 118, 123.3, 115, 108, 104.5]` → REGRESSING (nicht STAGNATING).
- `ReviewContextBuilderTest` erweitern (Ziel aus Routine der letzten Einheit, freie Einheit → null, Pausen).
- `SuggestionChecksTest`: jede Regel inkl. Ausnahmen; der Screenshot-Fall (Brust 7,8, Status STAGNATING, set_count 4 → 3) wird verworfen.
- `SuggestionJsonTest`/`ReviewParserTest`: `reason` gelesen, unbekannt → null, fehlt → null.
- `ReviewPromptTest`/`ChatPromptTest`: neue Felder und Version.
- `MigrationTest`: 4 → 5.
- UI: falls `CoachViewModelTest`/Screen-Tests das Chip-Mapping abdecken, ergänzen.

## Abschluss

- `./gradlew assembleDebug test lint` grün (Lint wie im CI).
- Keine hartcodierten Strings, keine dp/Hex in Composables.
- `docs/progress.md`: Eintrag unter „Erledigt“/„In Arbeit“, „Auf dem Pixel prüfen“ unter „Offen für Jonas“ (Review neu erstellen; Testdaten laden → Chips LEISTUNG/VOLUMEN/BELASTUNG, Formulierungen „Rückgang“ vs. „stagniert“; „Gesendete Daten ansehen“ zeigt `status`, `recent`).
- Commits auf Englisch, klein und thematisch.

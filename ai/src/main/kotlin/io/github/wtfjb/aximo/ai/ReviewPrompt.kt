package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.review.ReviewContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The weekly review prompt (B-02). Versioned: a change to the text or the
 * schema raises [VERSION]. The answer is parsed by [ReviewParser].
 */
object ReviewPrompt {
    const val VERSION = "review-v2"

    /** Most suggestions per review; more is noise. */
    const val MAX_SUGGESTIONS = 5

    fun request(context: ReviewContext, language: String): AiRequest = AiRequest(
        system = system(language),
        messages = listOf(AiMessage(AiMessage.Role.USER, contextJson(context).toString())),
    )

    fun system(language: String): String {
        val lang = languageName(language)
        return """
            You are the strength training coach in the app Aximo. The user message is JSON with aggregated training data of the
            last weeks and the user's current routines. Review it and propose concrete changes to the routines.

            Reply with one JSON object only, no markdown, no text around it:
            {
              "summary": string,
              "suggestions": [
                {"type": "set_count", "routine_id": int, "exercise_id": int, "from": int, "to": int, "rationale": string, "reason": string},
                {"type": "rep_range", "routine_id": int, "exercise_id": int, "from_min": int, "from_max": int, "to_min": int, "to_max": int, "rationale": string, "reason": string},
                {"type": "target_rir", "routine_id": int, "exercise_id": int, "from": int|null, "to": int|null, "rationale": string, "reason": string},
                {"type": "add_exercise", "routine_id": int, "exercise_id": int, "sets": int, "rep_min": int, "rep_max": int, "target_rir": int|null, "rationale": string, "reason": string},
                {"type": "remove_exercise", "routine_id": int, "exercise_id": int, "rationale": string, "reason": string}
              ]
            }

            Rules:
            - At most $MAX_SUGGESTIONS suggestions. An empty list is fine if nothing needs to change.
            - Use only routine_id and exercise_id values from the data. For add_exercise pick exercise_id from "available_exercises"
              and only if it is not in that routine yet.
            - "from" values must equal the current values in the routine.
            - Sets 1–10, reps 1–50, RIR 0–5.
            - Volume target: 10–20 working sets per body region and week (see "volume").
            - The app has already classified every exercise and region. Take these labels over, do not judge them again:
              "status" of an exercise: too_few_data, returning, regressing, stagnating, progressing, stable.
              "effort" of an exercise: too_hard, on_target, too_easy (average RIR of the last sessions against target_rir).
              Volume "status" per region: below, in_range, above (10–20 working sets per week).
            - Choose the words by status. regressing means the values fell (use "drop", give change_pct or drop_from_best_pct),
              stagnating means no new best without a real drop. Never call a regression stagnation.
              returning means the user came back after a break (days_since_last, longest_break_days): do not read a drop
              after a break as lost strength or as a reason to reduce.
            - If sessions_at_rep_ceiling ≥ 2 (all sets at rep_max, enough RIR left), say in summary or rationale that the weight
              can go up. There is no suggestion type for weight; do not invent one.
            - Do not lower set_count for an exercise whose primary region has volume status below, unless its status is
              regressing or its effort is too_hard. Do not raise set_count if a primary region is above. Do not add an exercise
              if all its regions are above. The app drops such suggestions.
            - Every suggestion has a "reason": progress, stagnation, regression, returning, volume_low, volume_high,
              effort_high, effort_low, rep_ceiling or other. Pick the one that caused the suggestion.
            - If an exercise has status progressing or new_best is true, the first sentence of the summary says so, naming the exercise.
            - Also weigh missed sessions (sessions_per_week against weekly_goal, days_since_last_session).
            - summary: 2–4 sentences in $lang. rationale: 1–2 sentences in $lang that name the numbers they rely on,
              e.g. "9 sets per week, target 10–20". Plain tone, no exclamation marks. Weights are in kg.
        """.trimIndent()
    }

    /** The user message: the context as compact JSON with snake_case keys. */
    fun contextJson(context: ReviewContext): JsonObject = buildJsonObject {
        put("prompt_version", VERSION)
        put("today", context.today.toString())
        put("weeks", context.weeks)
        put("sessions", context.sessions)
        put("sessions_per_week", context.sessionsPerWeek)
        put("weekly_goal", context.weeklyGoal)
        put("unit", "kg")
        put("days_since_last_session", context.daysSinceLastSession)
        put("longest_break_days", context.longestBreakDays)
        putJsonArray("routines") {
            context.routines.forEach { r ->
                addJsonObject {
                    put("routine_id", r.routineId)
                    put("name", r.name)
                    put("sessions_in_period", r.sessions)
                    putJsonArray("exercises") {
                        r.exercises.forEach { e ->
                            addJsonObject {
                                put("exercise_id", e.exerciseId)
                                put("name", e.name)
                                put("sets", e.sets)
                                put("rep_min", e.repMin)
                                put("rep_max", e.repMax)
                                put("target_rir", e.targetRir)
                            }
                        }
                    }
                }
            }
        }
        putJsonArray("exercise_trends") {
            context.exercises.forEach { t ->
                addJsonObject {
                    put("exercise_id", t.exerciseId)
                    put("name", t.name)
                    put("sessions", t.sessions)
                    put("metric", if (t.metric.name == "E1RM") "e1rm_kg" else "max_reps")
                    put("first", t.first)
                    put("last", t.last)
                    put("best", t.best)
                    put("sessions_since_best", t.sessionsSinceBest)
                    put("last_top_set", "${formatKg(t.lastTopWeightKg)} kg x ${t.lastTopReps}")
                    put("avg_rir", t.avgRir)
                    putJsonArray("recent") { t.recent.forEach { add(JsonPrimitive(it)) } }
                    put("change_pct", t.changePct)
                    put("drop_from_best_pct", t.dropFromBestPct)
                    put("days_since_last", t.daysSinceLast)
                    put("status", t.status.name.lowercase())
                    put("new_best", t.newBest)
                    put("rep_max", t.repMax)
                    put("target_rir", t.targetRir)
                    put("sessions_at_rep_ceiling", t.sessionsAtRepCeiling)
                    put("recent_avg_rir", t.recentAvgRir)
                    put("rir_vs_target", t.rirVsTarget)
                    put("effort", t.effort?.name?.lowercase())
                }
            }
        }
        putJsonObject("volume") {
            put("target_min", context.volume.firstOrNull()?.targetMin ?: 10.0)
            put("target_max", context.volume.firstOrNull()?.targetMax ?: 20.0)
            putJsonObject("sets_per_week") {
                context.volume.forEach { v ->
                    putJsonObject(v.region.name.lowercase()) {
                        put("sets", v.setsPerWeek)
                        put("status", v.status.name.lowercase())
                    }
                }
            }
        }
        putJsonObject("effort") {
            put("avg_rir_first_half", context.effort.avgRirFirstHalf)
            put("avg_rir_second_half", context.effort.avgRirSecondHalf)
        }
        putJsonArray("available_exercises") {
            context.available.forEach { a ->
                addJsonObject {
                    put("exercise_id", a.exerciseId)
                    put("name", a.name)
                    putJsonArray("regions") { a.regions.sortedBy { it.ordinal }.forEach { add(JsonPrimitive(it.name.lowercase())) } }
                }
            }
        }
    }

    /** The context as the user sees it under "Gesendete Daten ansehen": same JSON, indented. */
    fun prettyContext(context: ReviewContext): String =
        prettyJson.encodeToString(JsonObject.serializer(), contextJson(context))

    private val prettyJson = Json { prettyPrint = true }

    private fun formatKg(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()

    private fun languageName(tag: String): String = when (tag.lowercase().take(2)) {
        "de" -> "German"
        else -> "English"
    }
}

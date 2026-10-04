package io.github.wtfjb.aximo.domain.plan

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.first

/**
 * Plan generation (B-03): asks the AI for routines and returns a draft. Nothing
 * is stored until the user confirms and [save] is called; the AI never writes data.
 */
class PlanService(
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val profiles: AiProfileRepository,
    private val factory: AiProviderFactory,
    private val generator: PlanGenerator,
) {

    /** Exactly what would be sent for [request]. */
    suspend fun input(request: PlanRequest): PlanInput =
        PlanInput.from(request, exercises.observeExercises(includeArchived = false).first())

    /** Asks the active profile for a plan. Throws [PlanException] or [AiException]. */
    suspend fun generate(request: PlanRequest, language: String): PlanProposal {
        val profile = AiProfiles.active(profiles.observeProfiles().first())
            ?: throw PlanException(PlanException.Reason.NO_PROFILE)
        val input = input(request)
        if (input.exercises.size < PlanInput.MIN_EXERCISES) throw PlanException(PlanException.Reason.NOT_ENOUGH_EXERCISES)

        val provider = factory.create(profile, profiles.apiKey(profile.id))
        return PlanBuilder.build(generator.generate(provider, input, language), input)
    }

    /** Saves the confirmed routines at the end of the list; returns their ids. */
    suspend fun save(proposal: PlanProposal): List<Long> {
        var position = (routines.observeRoutines().first().maxOfOrNull { it.position } ?: -1) + 1
        return proposal.routines.map { routine ->
            routines.saveRoutine(Routine(name = routine.name, position = position++), routine.toEntries())
        }
    }
}

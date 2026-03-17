package codes.bed.minestom.mobs.builder

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathingBundle
import codes.bed.minestom.mobs.ai.pathfinding.PathTraversalOptions
import codes.bed.minestom.mobs.api.GoalFactory
import codes.bed.minestom.mobs.api.MobLifecycleHook
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.api.TargetFactory

/**
 * Java-friendly builder used to assemble immutable [MobProfile] instances.
 */
class MobProfileBuilder private constructor(
    private val id: String,
    private val entityType: EntityType
) {
    private var stats: MobStats = MobStats.fixed(
        health = 20.0,
        attack = 2.0,
        moveSpeed = 0.25
    )

    private val goals = mutableListOf<GoalFactory>()
    private val targets = mutableListOf<TargetFactory>()
    private var traversalOptions: PathTraversalOptions = PathTraversalOptions()

    private var onSpawn: MobLifecycleHook = MobLifecycleHook {}
    private var onTick: MobLifecycleHook = MobLifecycleHook {}
    private var onDeath: MobLifecycleHook = MobLifecycleHook {}
    private var onAttack: MobLifecycleHook = MobLifecycleHook {}

    /** Sets the stat scaling model for the profile. */
    fun stats(stats: MobStats): MobProfileBuilder = apply { this.stats = stats }

    /** Adds a goal selector factory to the profile. */
    fun goal(factory: GoalFactory): MobProfileBuilder = apply { goals += factory }

    /** Adds a target selector factory to the profile. */
    fun target(factory: TargetFactory): MobProfileBuilder = apply { targets += factory }

    /** Adds all goals and targets from a pathing bundle. */
    fun applyPathing(bundle: PathingBundle): MobProfileBuilder = apply {
        goals += bundle.goals
        targets += bundle.targets
        traversalOptions = bundle.traversalOptions
    }

    /** Registers a spawn lifecycle hook. */
    fun onSpawn(hook: MobLifecycleHook): MobProfileBuilder = apply { onSpawn = hook }

    /** Registers a tick lifecycle hook. */
    fun onTick(hook: MobLifecycleHook): MobProfileBuilder = apply { onTick = hook }

    /** Registers a death lifecycle hook. */
    fun onDeath(hook: MobLifecycleHook): MobProfileBuilder = apply { onDeath = hook }

    /** Registers an attack lifecycle hook. */
    fun onAttack(hook: MobLifecycleHook): MobProfileBuilder = apply { onAttack = hook }

    /** Finalizes and returns an immutable [MobProfile]. */
    fun build(): MobProfile = MobProfile(
        id = id,
        entityType = entityType,
        stats = stats,
        goals = goals.toList(),
        targets = targets.toList(),
        traversalOptions = traversalOptions,
        onSpawn = onSpawn,
        onTick = onTick,
        onDeath = onDeath,
        onAttack = onAttack
    )

    companion object {
        /** Creates a new profile builder for the given id and Minestom entity type. */
        @JvmStatic
        fun create(id: String, entityType: EntityType): MobProfileBuilder {
            require(id.isNotBlank()) { "Mob profile id cannot be blank." }
            return MobProfileBuilder(id = id, entityType = entityType)
        }
    }
}



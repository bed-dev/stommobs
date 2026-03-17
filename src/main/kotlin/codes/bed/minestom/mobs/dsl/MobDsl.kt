package codes.bed.minestom.mobs.dsl

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathingBundle
import codes.bed.minestom.mobs.api.GoalFactory
import codes.bed.minestom.mobs.api.MobLifecycleHook
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.api.TargetFactory
import codes.bed.minestom.mobs.builder.MobProfileBuilder

/** Kotlin DSL wrapper around [MobProfileBuilder]. */
class MobProfileDsl internal constructor(
    private val id: String,
    private val entityType: EntityType
) {
    private val builder = MobProfileBuilder.create(id, entityType)

    fun stats(stats: MobStats) {
        builder.stats(stats)
    }

    fun goal(factory: GoalFactory) {
        builder.goal(factory)
    }

    fun target(factory: TargetFactory) {
        builder.target(factory)
    }

    fun pathing(bundle: PathingBundle) {
        builder.applyPathing(bundle)
    }

    fun onSpawn(hook: MobLifecycleHook) {
        builder.onSpawn(hook)
    }

    fun onTick(hook: MobLifecycleHook) {
        builder.onTick(hook)
    }

    fun onDeath(hook: MobLifecycleHook) {
        builder.onDeath(hook)
    }

    fun onAttack(hook: MobLifecycleHook) {
        builder.onAttack(hook)
    }

    /** Builds the configured profile. */
    fun build(): MobProfile = builder.build()
}

/**
 * Creates a [MobProfile] using idiomatic Kotlin DSL configuration.
 */
fun mobProfile(id: String, entityType: EntityType, block: MobProfileDsl.() -> Unit): MobProfile {
    val dsl = MobProfileDsl(id = id, entityType = entityType)
    dsl.block()
    return dsl.build()
}



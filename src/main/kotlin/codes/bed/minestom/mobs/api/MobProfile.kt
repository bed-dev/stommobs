package codes.bed.minestom.mobs.api

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathTraversalOptions

/**
 * Immutable mob definition used by [codes.bed.minestom.mobs.spawn.MobSpawner].
 *
 * A profile contains entity type, stat model, AI selectors and optional lifecycle hooks.
 */
data class MobProfile(
    val id: String,
    val entityType: EntityType,
    val stats: MobStats,
    val goals: List<GoalFactory>,
    val targets: List<TargetFactory>,
    val traversalOptions: PathTraversalOptions = PathTraversalOptions(),
    val onSpawn: MobLifecycleHook = MobLifecycleHook {},
    val onTick: MobLifecycleHook = MobLifecycleHook {},
    val onDeath: MobLifecycleHook = MobLifecycleHook {},
    val onAttack: MobLifecycleHook = MobLifecycleHook {}
)



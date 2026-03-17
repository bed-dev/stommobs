package codes.bed.minestom.mobs.dsl

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.instance.Instance
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.spawn.MobSpawner

/** Configuration object used by [MobSpawner.spawnMob]. */
class SpawnDsl internal constructor() {
    var profileId: String? = null
    var profile: MobProfile? = null
    lateinit var instance: Instance
    lateinit var position: Pos
    var level: Int = 1
}

/**
 * Spawns a mob using Kotlin DSL syntax.
 *
 * Provide either [SpawnDsl.profile] or [SpawnDsl.profileId].
 */
fun MobSpawner.spawnMob(block: SpawnDsl.() -> Unit): EntityCreature {
    val dsl = SpawnDsl().apply(block)
    val profile = dsl.profile
    val normalizedLevel = if (dsl.level < 1) 1 else dsl.level

    return if (profile != null) {
        spawn(profile, dsl.instance, dsl.position, normalizedLevel)
    } else {
        val profileId = requireNotNull(dsl.profileId) {
            "spawnMob DSL requires either profile or profileId"
        }
        spawn(profileId, dsl.instance, dsl.position, normalizedLevel)
    }
}

/**
 * Kotlin convenience overload for id-based spawning without mutable DSL fields.
 */
fun MobSpawner.spawnMob(
    profileId: String,
    instance: Instance,
    position: Pos,
    level: Int = 1
): EntityCreature {
    return spawn(profileId, instance, position, level)
}

/**
 * Kotlin convenience overload for profile-based spawning without mutable DSL fields.
 */
fun MobSpawner.spawnMob(
    profile: MobProfile,
    instance: Instance,
    position: Pos,
    level: Int = 1
): EntityCreature {
    return spawn(profile, instance, position, level)
}

/** Alias for [spawnMob] DSL style. */
fun MobSpawner.spawnDsl(block: SpawnDsl.() -> Unit): EntityCreature = spawnMob(block)



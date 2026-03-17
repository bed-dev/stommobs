package codes.bed.minestom.mobs.api

import net.minestom.server.entity.EntityCreature
import codes.bed.minestom.mobs.spawn.MobSpawner

data class MobContext(
    val spawner: MobSpawner,
    val profile: MobProfile,
    val entity: EntityCreature,
    val level: Int,
    val tick: Long
)


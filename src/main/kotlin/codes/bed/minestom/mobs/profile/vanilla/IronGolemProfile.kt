package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

/**
 * Village defender style AI: targets hostile mobs but ignores players.
 */
object IronGolemProfile {
    private val hostileTypes = setOf(
        EntityType.ZOMBIE,
        EntityType.HUSK,
        EntityType.DROWNED,
        EntityType.SKELETON,
        EntityType.STRAY,
        EntityType.SPIDER,
        EntityType.CREEPER
    )

    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:iron_golem", EntityType.IRON_GOLEM)
            .stats(
                MobStats(
                    baseHealth = 100.0,
                    healthPerLevel = 8.0,
                    baseAttack = 15.0,
                    attackPerLevel = 2.0,
                    baseMoveSpeed = 0.18,
                    speedPerLevel = 0.002,
                    baseArmor = 8.0,
                    armorPerLevel = 0.35,
                    baseFollowRange = 28.0,
                    followRangePerLevel = 0.2
                )
            )
            .applyPathing(
                PathingPresets.hostileMelee(
                    speed = 0.7,
                    attackDelay = 30,
                    searchRange = 18.0,
                    includeLastDamager = false,
                    lineOfSightRequired = true,
                    enableWander = true,
                    strollChance = 6,
                    wanderStartChancePerTick = 360,
                    targetFilter = { target ->
                        target !is Player && target.entityType in hostileTypes
                    }
                )
            )
            .onTick { context ->
                if (context.entity.target == null) {
                    context.entity.setView(context.entity.position.yaw(), 0f)
                }
            }
            .build()
    }
}


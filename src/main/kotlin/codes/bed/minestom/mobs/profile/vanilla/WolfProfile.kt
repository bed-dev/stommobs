package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

/**
 * Wolf-like hunter: wanders but will hunt passive farm mobs.
 */
object WolfProfile {
    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:wolf", EntityType.WOLF)
            .stats(
                MobStats(
                    baseHealth = 16.0,
                    healthPerLevel = 2.0,
                    baseAttack = 4.0,
                    attackPerLevel = 0.7,
                    baseMoveSpeed = 0.24,
                    speedPerLevel = 0.004,
                    baseArmor = 1.0,
                    armorPerLevel = 0.1,
                    baseFollowRange = 20.0,
                    followRangePerLevel = 0.15
                )
            )
            .applyPathing(
                PathingPresets.hostileMelee(
                    speed = 0.8,
                    attackDelay = 26,
                    searchRange = 14.0,
                    includeLastDamager = false,
                    lineOfSightRequired = true,
                    enableWander = true,
                    strollChance = 6,
                    wanderStartChancePerTick = 240,
                    targetFilter = { target ->
                        target.entityType == EntityType.SHEEP || target.entityType == EntityType.RABBIT
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


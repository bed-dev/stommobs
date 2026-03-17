package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

/**
 * Fast ambusher profile focused on players.
 */
object EndermanProfile {
    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:enderman", EntityType.ENDERMAN)
            .stats(
                MobStats(
                    baseHealth = 40.0,
                    healthPerLevel = 5.0,
                    baseAttack = 7.0,
                    attackPerLevel = 1.1,
                    baseMoveSpeed = 0.24,
                    speedPerLevel = 0.005,
                    baseArmor = 0.0,
                    armorPerLevel = 0.0,
                    baseFollowRange = 26.0,
                    followRangePerLevel = 0.25
                )
            )
            .applyPathing(
                PathingPresets.hostileMelee(
                    speed = 0.85,
                    attackDelay = 26,
                    searchRange = 18.0,
                    includeLastDamager = false,
                    lineOfSightRequired = true,
                    enableWander = true,
                    strollChance = 6,
                    wanderStartChancePerTick = 320,
                    targetFilter = { target -> target is Player }
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


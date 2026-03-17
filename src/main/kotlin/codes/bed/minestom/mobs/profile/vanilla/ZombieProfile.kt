package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

object ZombieProfile {
    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:zombie", EntityType.ZOMBIE)
            .stats(
                MobStats(
                    baseHealth = 20.0,
                    healthPerLevel = 6.0,
                    baseAttack = 4.0,
                    attackPerLevel = 1.2,
                    baseMoveSpeed = 0.20,
                    speedPerLevel = 0.004,
                    baseArmor = 2.0,
                    armorPerLevel = 0.35,
                    baseFollowRange = 24.0,
                    followRangePerLevel = 0.15
                )
            )
            .applyPathing(
                PathingPresets.hostileMelee(
                    speed = 0.95,
                    attackDelay = 24,
                    searchRange = 16.0,
                    includeLastDamager = false,
                    lineOfSightRequired = true,
                    allowLadders = true,
                    enableWander = false,
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


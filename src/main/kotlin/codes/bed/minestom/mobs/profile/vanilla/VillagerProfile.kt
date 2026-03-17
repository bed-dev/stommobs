package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

object VillagerProfile {
    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:villager", EntityType.VILLAGER)
            .stats(
                MobStats(
                    baseHealth = 20.0,
                    healthPerLevel = 2.0,
                    baseAttack = 1.0,
                    attackPerLevel = 0.1,
                    baseMoveSpeed = 0.30,
                    speedPerLevel = 0.003,
                    baseArmor = 0.0,
                    armorPerLevel = 0.0,
                    baseFollowRange = 16.0,
                    followRangePerLevel = 0.1
                )
            )
            .applyPathing(
                PathingPresets.passiveWander(
                    strollRadius = 8,
                    wanderStartChancePerTick = 170,
                    // Passive mobs avoid hazards while calm, but can cross them if aggroed.
                    avoidHazardsWhenCalm = true,
                    avoidHazardsWhenAggressive = false,
                    allowLadders = false,
                    allowSwimming = false
                )
            )
            .build()
    }
}


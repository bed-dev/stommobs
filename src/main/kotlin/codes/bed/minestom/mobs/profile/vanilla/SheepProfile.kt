package codes.bed.minestom.mobs.profile.vanilla

import net.minestom.server.entity.EntityType
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.builder.MobProfileBuilder

object SheepProfile {
    @JvmStatic
    fun create(): MobProfile {
        return MobProfileBuilder.create("vanilla:sheep", EntityType.SHEEP)
            .stats(
                MobStats(
                    baseHealth = 8.0,
                    healthPerLevel = 1.0,
                    baseAttack = 1.0,
                    attackPerLevel = 0.0,
                    baseMoveSpeed = 0.18,
                    speedPerLevel = 0.002,
                    baseArmor = 0.0,
                    armorPerLevel = 0.0,
                    baseFollowRange = 14.0,
                    followRangePerLevel = 0.1
                )
            )
            .applyPathing(
                PathingPresets.passiveWander(
                    strollRadius = 7,
                    wanderStartChancePerTick = 380,
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


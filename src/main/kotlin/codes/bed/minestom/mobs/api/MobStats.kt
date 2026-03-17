package codes.bed.minestom.mobs.api

import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.attribute.Attribute
import kotlin.math.max

/**
 * Per-profile combat and movement attributes with linear level growth.
 *
 * Level scaling starts at level 1, where each stat equals its base value.
 */
data class MobStats(
    val baseHealth: Double,
    val healthPerLevel: Double = 0.0,
    val baseAttack: Double,
    val attackPerLevel: Double = 0.0,
    val baseMoveSpeed: Double,
    val speedPerLevel: Double = 0.0,
    val baseArmor: Double = 0.0,
    val armorPerLevel: Double = 0.0,
    val baseFollowRange: Double = 24.0,
    val followRangePerLevel: Double = 0.0
) {
    /** Returns level-scaled max health. */
    fun health(level: Int): Double = scaled(baseHealth, healthPerLevel, level)

    /** Returns level-scaled attack damage. */
    fun attack(level: Int): Double = scaled(baseAttack, attackPerLevel, level)

    /** Returns level-scaled movement speed. */
    fun moveSpeed(level: Int): Double = scaled(baseMoveSpeed, speedPerLevel, level)

    /** Returns level-scaled armor value. */
    fun armor(level: Int): Double = scaled(baseArmor, armorPerLevel, level)

    /** Returns level-scaled follow range. */
    fun followRange(level: Int): Double = scaled(baseFollowRange, followRangePerLevel, level)

    /** Applies all tracked attributes onto the provided Minestom creature. */
    fun applyTo(creature: EntityCreature, level: Int) {
        creature.getAttribute(Attribute.MAX_HEALTH).baseValue = health(level)
        creature.getAttribute(Attribute.ATTACK_DAMAGE).baseValue = attack(level)
        creature.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = moveSpeed(level)
        creature.getAttribute(Attribute.ARMOR).baseValue = armor(level)
        creature.getAttribute(Attribute.FOLLOW_RANGE).baseValue = followRange(level)
        creature.health = health(level).toFloat()
    }

    private fun scaled(base: Double, growth: Double, level: Int): Double {
        val normalized = max(1, level) - 1
        return base + (growth * normalized)
    }

    companion object {
        /**
         * Convenience helper for creating non-scaling stat sets.
         */
        @JvmStatic
        fun fixed(
            health: Double,
            attack: Double,
            moveSpeed: Double,
            armor: Double = 0.0,
            followRange: Double = 24.0
        ): MobStats = MobStats(
            baseHealth = health,
            baseAttack = attack,
            baseMoveSpeed = moveSpeed,
            baseArmor = armor,
            baseFollowRange = followRange
        )
    }
}



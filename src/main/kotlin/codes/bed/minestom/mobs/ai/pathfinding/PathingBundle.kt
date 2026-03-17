package codes.bed.minestom.mobs.ai.pathfinding

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.goal.MeleeAttackGoal
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.time.TimeUnit
import codes.bed.minestom.mobs.api.GoalFactory
import codes.bed.minestom.mobs.api.TargetFactory
import kotlin.math.abs
import kotlin.math.max

/**
 * Minestom pathing bundle that groups related goals and target selectors.
 */
data class PathingBundle(
    val goals: List<GoalFactory>,
    val targets: List<TargetFactory>,
    val traversalOptions: PathTraversalOptions = PathTraversalOptions()
)

/**
 * Path traversal knobs for hazards and special movement blocks.
 */
data class PathTraversalOptions(
    val avoidHazardsWhenCalm: Boolean = false,
    val avoidHazardsWhenAggressive: Boolean = false,
    val allowLadders: Boolean = false,
    val allowSwimming: Boolean = false,
    val allowDropWhenFocused: Boolean = false,
    val maxDropHeight: Int = 4
)

fun interface HazardPolicy {
    fun isHazard(block: Block): Boolean
}

class MutableHazardPolicy private constructor(
    private val blockedTokens: MutableSet<String>
) : HazardPolicy {

    override fun isHazard(block: Block): Boolean {
        val name = block.name().lowercase()
        return blockedTokens.any { token -> name.contains(token) }
    }

    fun addToken(token: String): MutableHazardPolicy = apply {
        if (token.isNotBlank()) blockedTokens += token.lowercase()
    }

    fun removeToken(token: String): MutableHazardPolicy = apply {
        blockedTokens -= token.lowercase()
    }

    fun clear(): MutableHazardPolicy = apply { blockedTokens.clear() }

    fun tokens(): Set<String> = blockedTokens.toSet()

    companion object {
        @JvmStatic
        fun default(): MutableHazardPolicy = MutableHazardPolicy(
            mutableSetOf("magma")
        )

        @JvmStatic
        fun none(): MutableHazardPolicy = MutableHazardPolicy(mutableSetOf())
    }
}

/** Built-in pathfinding/targeting presets backed by Minestom selectors. */
object PathingPresets {

    /** Mutable default policy; callers can add/remove hazard tokens globally. */
    @JvmField
    val defaultHazardPolicy: MutableHazardPolicy = MutableHazardPolicy.default()

    /**
     * Hostile melee preset with attack + wander goals and configurable target selectors.
     */
    @JvmStatic
    fun hostileMelee(
        speed: Double = 1.35,
        attackDelay: Int = 20,
        searchRange: Double = 24.0,
        includeLastDamager: Boolean = true,
        lineOfSightRequired: Boolean = true,
        strollChance: Int = 8,
        wanderStartChancePerTick: Int = 180,
        ignoreCreativePlayers: Boolean = true,
        ignoreSpectatorPlayers: Boolean = true,
        avoidMagmaAndTrapdoors: Boolean = false,
        avoidHazardsWhenCalm: Boolean = avoidMagmaAndTrapdoors,
        avoidHazardsWhenAggressive: Boolean = avoidMagmaAndTrapdoors,
        allowLadders: Boolean = false,
        allowSwimming: Boolean = false,
        enableWander: Boolean = true,
        hazardPolicy: HazardPolicy = defaultHazardPolicy,
        targetFilter: (Entity) -> Boolean = { entity -> entity is Player }
    ): PathingBundle {
        val goals = mutableListOf<GoalFactory>()
        goals += GoalFactory { creature ->
            MeleeAttackGoal(creature, speed, attackDelay, TimeUnit.SERVER_TICK)
        }
        if (enableWander && strollChance > 0) {
            goals += GoalFactory { creature ->
                RareRandomStrollGoal(
                    entityCreature = creature,
                    radius = strollChance,
                    startChancePerTick = wanderStartChancePerTick
                )
            }
        }

        val targets = mutableListOf<TargetFactory>()
        if (includeLastDamager) {
            targets += TargetFactory { creature -> LastEntityDamagerTarget(creature, searchRange.toFloat()) }
        }
        targets += TargetFactory { creature ->
            ClosestEntityTarget(creature, searchRange) { target ->
                val withinDirectRange = squaredDistance(creature.position, target.position) <= (searchRange * searchRange)
                val gameModeAllowed = isGameModeAllowed(target, ignoreCreativePlayers, ignoreSpectatorPlayers)
                val canSee = !lineOfSightRequired ||
                    creature.hasLineOfSight(target) ||
                    hasVerticalEdgeVisibility(creature, target)
                val avoidHazards = if (creature.target == null) avoidHazardsWhenCalm else avoidHazardsWhenAggressive
                val safePath = !avoidHazards || !hasHazardBetween(creature, target, hazardPolicy)
                withinDirectRange && gameModeAllowed && canSee && safePath && targetFilter(target)
            }
        }

        return PathingBundle(
            goals = goals.toList(),
            targets = targets,
            traversalOptions = PathTraversalOptions(
                avoidHazardsWhenCalm = avoidHazardsWhenCalm,
                avoidHazardsWhenAggressive = avoidHazardsWhenAggressive,
                allowLadders = allowLadders,
                allowSwimming = allowSwimming,
                allowDropWhenFocused = true,
                maxDropHeight = 4
            )
        )
    }

    /** Passive wandering preset without hostile targets. */
    @JvmStatic
    fun passiveWander(
        strollRadius: Int = 8,
        wanderStartChancePerTick: Int = 220,
        avoidHazardsWhenCalm: Boolean = true,
        avoidHazardsWhenAggressive: Boolean = false,
        allowLadders: Boolean = false,
        allowSwimming: Boolean = false
    ): PathingBundle = PathingBundle(
        goals = listOf(
            GoalFactory { creature ->
                RareRandomStrollGoal(
                    entityCreature = creature,
                    radius = strollRadius,
                    startChancePerTick = wanderStartChancePerTick
                )
            }
        ),
        targets = emptyList(),
        traversalOptions = PathTraversalOptions(
            avoidHazardsWhenCalm = avoidHazardsWhenCalm,
            avoidHazardsWhenAggressive = avoidHazardsWhenAggressive,
            allowLadders = allowLadders,
            allowSwimming = allowSwimming,
            allowDropWhenFocused = false,
            maxDropHeight = 3
        )
    )

    private fun isGameModeAllowed(target: Entity, ignoreCreative: Boolean, ignoreSpectator: Boolean): Boolean {
        val player = target as? Player ?: return true
        if (ignoreCreative && player.gameMode == GameMode.CREATIVE) return false
        if (ignoreSpectator && player.gameMode == GameMode.SPECTATOR) return false
        return true
    }

    private fun hasHazardBetween(creature: EntityCreature, target: Entity, hazardPolicy: HazardPolicy): Boolean {
        val instance = creature.instance ?: return false
        val from = creature.position
        val to = target.position

        val dx = to.x() - from.x()
        val dz = to.z() - from.z()
        val maxAxis = max(kotlin.math.abs(dx), kotlin.math.abs(dz))
        val steps = max(1, maxAxis.toInt())

        for (step in 0..steps) {
            val ratio = step.toDouble() / steps.toDouble()
            val x = from.x() + (dx * ratio)
            val z = from.z() + (dz * ratio)
            val check = Pos(x, from.y() - 1.0, z)
            val block = instance.getBlock(check)
            if (hazardPolicy.isHazard(block)) {
                return true
            }
        }
        return false
    }

    private fun squaredDistance(
        from: net.minestom.server.coordinate.Point,
        to: net.minestom.server.coordinate.Point
    ): Double {
        val dx = from.x() - to.x()
        val dy = from.y() - to.y()
        val dz = from.z() - to.z()
        return (dx * dx) + (dy * dy) + (dz * dz)
    }

    /**
     * Helps hostile mobs keep focus when the player is just above a ledge/top-block edge.
     */
    private fun hasVerticalEdgeVisibility(creature: EntityCreature, target: Entity): Boolean {
        val from = creature.position
        val to = target.position
        val dx = to.x() - from.x()
        val dz = to.z() - from.z()
        val horizontalSq = (dx * dx) + (dz * dz)
        if (horizontalSq > (3.0 * 3.0)) return false

        val dy = to.y() - from.y()
        return dy in -1.0..4.5 && abs(dx) <= 3.0 && abs(dz) <= 3.0
    }
}

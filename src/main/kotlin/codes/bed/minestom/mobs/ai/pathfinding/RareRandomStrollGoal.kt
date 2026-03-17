package codes.bed.minestom.mobs.ai.pathfinding

import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.entity.ai.goal.RandomStrollGoal
import net.minestom.server.tag.Tag
import java.util.concurrent.ThreadLocalRandom

/**
 * Random stroll wrapper that starts rarely to mimic more natural idle roaming.
 */
class RareRandomStrollGoal(
    entityCreature: EntityCreature,
    radius: Int,
    private val startChancePerTick: Int
) : GoalSelector(entityCreature) {
    private val delegate = RandomStrollGoal(entityCreature, radius.coerceIn(4, 14))

    override fun shouldStart(): Boolean {
        // Do not stroll if there is a combat target or if manual movement is active.
        if (entityCreature.target != null) return false
        if (entityCreature.hasTag(Tag.Boolean("stommobs:manual_move"))) return false

        val gate = if (startChancePerTick <= 1) 1 else startChancePerTick
        if (ThreadLocalRandom.current().nextInt(gate) != 0) return false
        return delegate.shouldStart()
    }

    override fun start() {
        delegate.start()
    }

    override fun tick(time: Long) {
        delegate.tick(time)
    }

    override fun shouldEnd(): Boolean {
        return delegate.shouldEnd()
    }

    override fun end() {
        delegate.end()
    }
}

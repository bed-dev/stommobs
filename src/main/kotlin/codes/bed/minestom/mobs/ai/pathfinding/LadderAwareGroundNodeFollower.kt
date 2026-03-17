package codes.bed.minestom.mobs.ai.pathfinding

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.pathfinding.followers.GroundNodeFollower

/**
 * Ground follower that adds upward motion when traversing ladder-like blocks.
 */
class LadderAwareGroundNodeFollower(
    private val entity: Entity,
    private val allowLadders: Boolean,
    private val allowSwimming: Boolean
) : GroundNodeFollower(entity) {

    override fun moveTowards(current: Point, speed: Double, next: Point) {
        super.moveTowards(current, speed, next)

        val instance = entity.instance ?: return
        val currentPos = entity.position

        val feet = instance.getBlock(currentPos)
        val head = instance.getBlock(currentPos.add(0.0, 1.0, 0.0))
        val inClimbable = allowLadders && (isLadderLike(feet.name()) || isLadderLike(head.name()))
        val wantsUpwardTravel = next.y() > currentPos.y() + 0.01 || current.y() > currentPos.y() + 0.01
        if (inClimbable && wantsUpwardTravel) {
            // Faster ladder climbing without increasing normal ground movement speed.
            entity.velocity = Vec(entity.velocity.x() * 0.20, 0.62, entity.velocity.z() * 0.20)
        }

        val inWater = allowSwimming && (isWaterLike(feet.name()) || isWaterLike(head.name()))
        if (inWater && next.y() > currentPos.y() + 0.05) {
            // Keep buoyancy while swimming towards higher path nodes.
            entity.velocity = Vec(entity.velocity.x(), 0.12, entity.velocity.z())
        }
    }

    private fun isLadderLike(blockName: String): Boolean {
        val name = blockName.lowercase()
        return name.contains("ladder") || name.contains("vine") || name.contains("scaffolding")
    }

    private fun isWaterLike(blockName: String): Boolean {
        val name = blockName.lowercase()
        return name.contains("water") || name.contains("bubble_column")
    }
}



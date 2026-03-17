package codes.bed.minestom.mobs.spawn

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityCreature
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag
import kotlin.math.abs

internal class LadderAssistService(
    private val distanceSquared: (Point, Point) -> Double
) {
    companion object {
        @JvmField
        val LADDER_STATE_TAG: Tag<String> = Tag.String("stommobs:ladder_state")
    }

    fun tryLadderAssist(entity: EntityCreature, target: Point): Boolean {
        if (target.y() <= entity.position.y() + 0.6) {
            entity.removeTag(LADDER_STATE_TAG)
            return false
        }
        val instance = entity.instance ?: return false

        val ex = entity.position.x()
        val ez = entity.position.z()
        val baseX = entity.position.blockX()
        val baseY = entity.position.blockY()
        val baseZ = entity.position.blockZ()

        var nearestLadder: Pos? = null
        var nearestDist = Double.MAX_VALUE

        for (x in (baseX - 5)..(baseX + 5)) {
            for (z in (baseZ - 5)..(baseZ + 5)) {
                for (y in (baseY - 3)..(baseY + 10)) {
                    val block = instance.getBlock(x, y, z)
                    if (!isLadderLike(block.name())) continue

                    val center = Pos(x + 0.5, y.toDouble(), z + 0.5)
                    val dx = center.x() - ex
                    val dz = center.z() - ez
                    val dy = kotlin.math.abs(center.y() - entity.position.y())
                    val dist = (dx * dx) + (dz * dz) + dy
                    if (dist < nearestDist) {
                        nearestDist = dist
                        nearestLadder = center
                    }
                }
            }
        }

        val ladder = nearestLadder ?: run {
            entity.setTag(LADDER_STATE_TAG, "no_ladder")
            return false
        }

        val targetOnLadder = isPositionOnLadder(instance, target)
        val topExit = findTopExit(instance, ladder, target)
        if (!targetOnLadder && topExit == null) {
            entity.setTag(LADDER_STATE_TAG, "no_top_path")
            return false
        }

        val xDiff = ladder.x() - ex
        val zDiff = ladder.z() - ez
        val yDiff = ladder.y() - entity.position.y()
        val horizontalDistSq = (xDiff * xDiff) + (zDiff * zDiff)
        val nearHorizontal = horizontalDistSq <= 2.25
        val nearVertical = abs(yDiff) <= 2.25

        // Same X/Z but ladder far above/below can otherwise trap the mob in attach state.
        if (nearHorizontal && !nearVertical) {
            entity.setTag(LADDER_STATE_TAG, "approach_vertical")
            val verticalApproach = Pos(ladder.x(), ladder.y(), ladder.z())
            return entity.navigator.setPathTo(verticalApproach)
        }

        val nearLadder = nearHorizontal && nearVertical
        if (!nearLadder) {
            entity.setTag(LADDER_STATE_TAG, "approach")
            val approachTargets = listOf(
                Pos(ladder.x(), entity.position.y(), ladder.z()),
                Pos(ladder.x(), ladder.y(), ladder.z()),
                Pos(ladder.x(), ladder.y() - 1.0, ladder.z()),
                Pos(ladder.x() + 1.0, ladder.y(), ladder.z()),
                Pos(ladder.x() - 1.0, ladder.y(), ladder.z()),
                Pos(ladder.x(), ladder.y(), ladder.z() + 1.0),
                Pos(ladder.x(), ladder.y(), ladder.z() - 1.0),
                Pos(ladder.x(), ladder.y() - 1.0, ladder.z())
            ).sortedBy { candidate -> distanceSquared(entity.position, candidate) }

            for (candidate in approachTargets) {
                if (entity.navigator.setPathTo(candidate)) {
                    return true
                }
            }
            return false
        }

        val touchingLadder = isTouchingLadder(instance, entity.position, ladder)
        if (!touchingLadder) {
            entity.setTag(LADDER_STATE_TAG, "attach")
            // Keep lateral push small so climb assist does not make mobs feel faster on flat movement.
            val attachX = (xDiff * 0.40).coerceIn(-0.10, 0.10)
            val attachZ = (zDiff * 0.40).coerceIn(-0.10, 0.10)
            val canClimbHere = hasClimbableAtOrAbove(instance, ladder, entity.position.blockY())
            val attachY = if (target.y() > entity.position.y() + 0.25 && horizontalDistSq < 1.2 && canClimbHere) {
                0.24
            } else {
                entity.velocity.y().coerceAtMost(0.0)
            }
            entity.velocity = Vec(attachX, attachY, attachZ)
            return true
        }

        // While attached, strongly lock to ladder center so the mob does not drift toward edge targets.
        entity.refreshPosition(Pos(ladder.x(), entity.position.y(), ladder.z(), entity.position.yaw(), entity.position.pitch()))

        val wantsUpward = target.y() > entity.position.y() + 0.25
        val canContinueUp = hasClimbableAbove(instance, ladder, entity.position.blockY())
        if (wantsUpward && !canContinueUp) {
            val exit = topExit
            if (exit != null) {
                entity.setTag(LADDER_STATE_TAG, "top_exit")
                val pushX = (exit.x() - entity.position.x()).coerceIn(-0.22, 0.22)
                val pushZ = (exit.z() - entity.position.z()).coerceIn(-0.22, 0.22)
                entity.velocity = Vec(pushX, 0.10, pushZ)
                entity.navigator.setPathTo(exit)
                return true
            }

            entity.setTag(LADDER_STATE_TAG, "top_release")
            entity.velocity = Vec(entity.velocity.x() * 0.60, entity.velocity.y().coerceAtMost(0.0), entity.velocity.z() * 0.60)
            return false
        }
        entity.setTag(LADDER_STATE_TAG, if (wantsUpward) "climb" else "hold")
        val climbY = if (wantsUpward) entity.velocity.y().coerceAtLeast(0.56) else entity.velocity.y().coerceAtMost(0.0)
        entity.velocity = Vec(entity.velocity.x() * 0.08, climbY, entity.velocity.z() * 0.08)
        return true
    }

    private fun hasClimbableAtOrAbove(instance: Instance, ladder: Pos, baseY: Int): Boolean {
        val lx = ladder.blockX()
        val lz = ladder.blockZ()
        for (y in baseY..(baseY + 2)) {
            if (isLadderLike(instance.getBlock(lx, y, lz).name())) {
                return true
            }
        }
        return false
    }

    private fun hasClimbableAbove(instance: Instance, ladder: Pos, baseY: Int): Boolean {
        val lx = ladder.blockX()
        val lz = ladder.blockZ()
        // Allow continuing climb if there is at least one climbable block at feet or above.
        for (y in baseY..(baseY + 2)) {
            if (isLadderLike(instance.getBlock(lx, y, lz).name())) {
                return true
            }
        }
        return false
    }

    private fun isPositionOnLadder(instance: Instance, position: Point): Boolean {
        val bx = position.blockX()
        val by = position.blockY()
        val bz = position.blockZ()
        val feet = instance.getBlock(bx, by, bz)
        val head = instance.getBlock(bx, by + 1, bz)
        return isLadderLike(feet.name()) || isLadderLike(head.name())
    }

    private fun findTopExit(instance: Instance, ladder: Pos, target: Point): Pos? {
        val lx = ladder.blockX()
        val lz = ladder.blockZ()
        var topY: Int? = null

        for (y in (ladder.blockY() - 3)..(ladder.blockY() + 24)) {
            if (isLadderLike(instance.getBlock(lx, y, lz).name())) {
                topY = y
            }
        }
        val top = topY ?: return null

        val directionPriority = listOf(
            Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)
        ).sortedByDescending { (dx, dz) ->
            val toTargetX = target.x() - (lx + 0.5)
            val toTargetZ = target.z() - (lz + 0.5)
            (toTargetX * dx) + (toTargetZ * dz)
        }

        // Try exits from higher to lower to support platforms one block above the top rung.
        for (standY in listOf(top + 2, top + 1, top)) {
            val floorY = standY - 1
            val feetY = standY
            val headY = standY + 1

            for ((dx, dz) in directionPriority) {
                val x = lx + dx
                val z = lz + dz
                val floor = instance.getBlock(x, floorY, z)
                val feet = instance.getBlock(x, feetY, z)
                val head = instance.getBlock(x, headY, z)

                val hasFloor = !floor.isAir && !isLadderLike(floor.name())
                val feetFree = feet.isAir || isWaterLike(feet.name())
                val headFree = head.isAir || isWaterLike(head.name())
                if (hasFloor && feetFree && headFree) {
                    return Pos(x + 0.5, standY.toDouble(), z + 0.5)
                }
            }
        }

        return null
    }

    private fun isTouchingLadder(instance: Instance, position: Pos, ladder: Pos): Boolean {
        val dx = ladder.x() - position.x()
        val dz = ladder.z() - position.z()
        val dy = abs(ladder.y() - position.y())
        if ((dx * dx) + (dz * dz) <= 0.55 && dy <= 1.35) {
            return true
        }
        val baseX = position.blockX()
        val baseY = position.blockY()
        val baseZ = position.blockZ()
        for (x in (baseX - 1)..(baseX + 1)) {
            for (z in (baseZ - 1)..(baseZ + 1)) {
                for (y in baseY..(baseY + 1)) {
                    val block = instance.getBlock(x, y, z)
                    if (isLadderLike(block.name())) {
                        return true
                    }
                }
            }
        }
        return false
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


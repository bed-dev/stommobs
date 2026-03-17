package codes.bed.minestom.mobs.ai.pathfinding

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.pathfinding.PNode
import net.minestom.server.entity.pathfinding.generators.GroundNodeGenerator
import net.minestom.server.instance.block.Block
import kotlin.math.abs

class HazardAwareGroundNodeGenerator(
    private val policyProvider: () -> HazardPolicy = { PathingPresets.defaultHazardPolicy },
    private val allowLadders: Boolean = false,
    private val allowSwimming: Boolean = false,
    private val maxDropHeight: Int = 4
) : GroundNodeGenerator() {

    private val directions = arrayOf(
        intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
    )

    override fun getWalkable(
        getter: Block.Getter,
        closed: Set<PNode>,
        current: PNode,
        goal: Point,
        boundingBox: BoundingBox
    ): Collection<PNode> {
        val base = super.getWalkable(getter, closed, current, goal, boundingBox)
        val policy = policyProvider()
        val filtered = base.filterNot { node ->
            val blockAtNode = getter.getBlock(node.blockX(), node.blockY(), node.blockZ())
            if (!allowLadders && isLadderLike(blockAtNode)) return@filterNot true
            if (!allowSwimming && isWaterLike(blockAtNode)) return@filterNot true

            val blockBelow = getter.getBlock(node.blockX(), node.blockY() - 1, node.blockZ())
            if (isTopTrapdoor(blockBelow)) return@filterNot false
            policy.isHazard(blockBelow)
        }.toMutableList()

        if (allowLadders) {
            val here = getter.getBlock(current.blockX(), current.blockY(), current.blockZ())
            val above = getter.getBlock(current.blockX(), current.blockY() + 1, current.blockZ())
            
            if (isLadderLike(here) || isLadderLike(above)) {
                val nextY = current.blockY() + 1
                val nextBlock = getter.getBlock(current.blockX(), nextY, current.blockZ())
                val headBlock = getter.getBlock(current.blockX(), nextY + 1, current.blockZ())
                
                if ((isLadderLike(nextBlock) || nextBlock.isAir) && (isLadderLike(headBlock) || headBlock.isAir)) {
                    val nodeX = current.blockX() + 0.5
                    val nodeZ = current.blockZ() + 0.5
                    val nextNode = PNode(
                        Pos(nodeX, nextY.toDouble(), nodeZ),
                        current.g() + 1.0,
                        heuristic(nodeX, nextY.toDouble(), nodeZ, goal),
                        PNode.Type.CLIMB,
                        current
                    )
                    if (nextNode !in closed && nextNode !in filtered) {
                        filtered += nextNode
                    }
                }
            }
        }

        addDropNodes(getter, current, goal, closed, filtered)
        addTrapdoorJumpNodes(getter, current, goal, closed, filtered)
        addStepUpJumpNodes(getter, current, goal, closed, filtered)

        return filtered
    }

    private fun addTrapdoorJumpNodes(
        getter: Block.Getter,
        current: PNode,
        goal: Point,
        closed: Set<PNode>,
        filtered: MutableList<PNode>
    ) {
        val baseX = current.blockX()
        val baseY = current.blockY()
        val baseZ = current.blockZ()

        for (dir in directions) {
            val nx = baseX + dir[0]
            val nz = baseZ + dir[1]
            val obstacle = getter.getBlock(nx, baseY, nz)
            if (!isBottomTrapdoorObstacle(obstacle)) continue

            val ny = baseY
            val landingHead = getter.getBlock(nx, ny + 1, nz)
            val landingFloor = getter.getBlock(nx, ny - 1, nz)
            if (!landingHead.isAir) continue
            val solidFloor = !landingFloor.isAir || isTopTrapdoor(landingFloor)
            if (!solidFloor) continue

            val nodeX = nx + 0.5
            val nodeZ = nz + 0.5
            val jumpNode = PNode(
                Pos(nodeX, ny.toDouble(), nodeZ),
                current.g() + 1.6,
                heuristic(nodeX, ny.toDouble(), nodeZ, goal),
                PNode.Type.JUMP,
                current
            )
            if (jumpNode !in closed && jumpNode !in filtered) {
                filtered += jumpNode
            }
        }
    }

    private fun addStepUpJumpNodes(
        getter: Block.Getter,
        current: PNode,
        goal: Point,
        closed: Set<PNode>,
        filtered: MutableList<PNode>
    ) {
        val baseX = current.blockX()
        val baseY = current.blockY()
        val baseZ = current.blockZ()

        for (dir in directions) {
            val nx = baseX + dir[0]
            val nz = baseZ + dir[1]
            val obstacle = getter.getBlock(nx, baseY, nz)

            val obstacleSolid = (!obstacle.isAir && !isBottomTrapdoorObstacle(obstacle)) || isTopTrapdoor(obstacle)
            if (!obstacleSolid) continue

            val jumpY = baseY + 1
            val jumpFeet = getter.getBlock(nx, jumpY, nz)
            val jumpHead = getter.getBlock(nx, jumpY + 1, nz)
            if (!jumpFeet.isAir || !jumpHead.isAir) continue

            val nodeX = nx + 0.5
            val nodeZ = nz + 0.5
            val jumpNode = PNode(
                Pos(nodeX, jumpY.toDouble(), nodeZ),
                current.g() + 1.8,
                heuristic(nodeX, jumpY.toDouble(), nodeZ, goal),
                PNode.Type.JUMP,
                current
            )
            if (jumpNode !in closed && jumpNode !in filtered) {
                filtered += jumpNode
            }
        }
    }

    private fun addDropNodes(
        getter: Block.Getter,
        current: PNode,
        goal: Point,
        closed: Set<PNode>,
        filtered: MutableList<PNode>
    ) {
        val baseX = current.blockX()
        val baseY = current.blockY()
        val baseZ = current.blockZ()

        for (dir in directions) {
            val nx = baseX + dir[0]
            val nz = baseZ + dir[1]

            val sideFeet = getter.getBlock(nx, baseY, nz)
            val sideHead = getter.getBlock(nx, baseY + 1, nz)
            if (!sideFeet.isAir || !sideHead.isAir) continue

            for (drop in 1..maxDropHeight.coerceAtLeast(1)) {
                val ny = baseY - drop
                val landingFeet = getter.getBlock(nx, ny, nz)
                val landingHead = getter.getBlock(nx, ny + 1, nz)
                val landingFloor = getter.getBlock(nx, ny - 1, nz)
                
                if (!landingFeet.isAir || !landingHead.isAir) continue
                if (landingFloor.isAir || isWaterLike(landingFloor)) continue

                val nodeX = nx + 0.5
                val nodeZ = nz + 0.5
                val nextNode = PNode(
                    Pos(nodeX, ny.toDouble(), nodeZ),
                    current.g() + (1.2 + drop),
                    heuristic(nodeX, ny.toDouble(), nodeZ, goal),
                    PNode.Type.FALL,
                    current
                )
                if (nextNode !in closed && nextNode !in filtered) {
                    filtered += nextNode
                }
                break
            }
        }
    }

    private fun isLadderLike(block: Block): Boolean {
        val name = block.name().lowercase()
        return name.contains("ladder") || name.contains("vine") || name.contains("scaffolding")
    }

    private fun isTrapdoor(block: Block): Boolean {
        return block.name().lowercase().contains("trapdoor")
    }

    private fun isTopTrapdoor(block: Block): Boolean {
        if (!isTrapdoor(block)) return false
        val half = block.getProperty("half") ?: return false
        val open = block.getProperty("open")?.equals("true", ignoreCase = true) == true
        return half.equals("top", ignoreCase = true) && !open
    }

    private fun isBottomTrapdoorObstacle(block: Block): Boolean {
        if (!isTrapdoor(block)) return false
        val half = block.getProperty("half") ?: return false
        val open = block.getProperty("open")?.equals("true", ignoreCase = true) == true
        return half.equals("bottom", ignoreCase = true) && !open
    }

    private fun isWaterLike(block: Block): Boolean {
        val name = block.name().lowercase()
        return name.contains("water") || name.contains("bubble_column")
    }

    private fun heuristic(x: Double, y: Double, z: Double, goal: Point): Double {
        return abs(goal.x() - x) + abs(goal.y() - y) + abs(goal.z() - z)
    }
}

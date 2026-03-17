package codes.bed.minestom.mobs.spawn

import net.minestom.server.coordinate.Pos
import java.util.UUID

sealed interface FollowDestination {
    data class Block(val position: Pos) : FollowDestination
    data class Entity(val uuid: UUID) : FollowDestination
}


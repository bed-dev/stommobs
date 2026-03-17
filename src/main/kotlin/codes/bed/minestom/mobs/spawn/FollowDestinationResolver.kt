package codes.bed.minestom.mobs.spawn

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class FollowDestinationResolver(
    private val activeByEntityId: ConcurrentHashMap<UUID, ActiveMob>
) {
    fun resolveLookPosition(holder: EntityCreature, destination: FollowDestination): Pos? {
        return when (destination) {
            is FollowDestination.Block -> destination.position.add(0.0, 0.5, 0.0)
            is FollowDestination.Entity -> {
                val player = MinecraftServer.getConnectionManager().onlinePlayers
                    .firstOrNull { online -> online.uuid == destination.uuid }
                if (player != null) {
                    return if (player.instance == holder.instance) player.position.add(0.0, 1.3, 0.0) else null
                }

                val tracked = activeByEntityId[destination.uuid]?.entity ?: return null
                if (tracked.instance != holder.instance) return null
                tracked.position.add(0.0, 1.1, 0.0)
            }
        }
    }

    fun resolvePosition(holder: EntityCreature, destination: FollowDestination): Pos? {
        return when (destination) {
            is FollowDestination.Block -> destination.position
            is FollowDestination.Entity -> {
                val player = MinecraftServer.getConnectionManager().onlinePlayers
                    .firstOrNull { online -> online.uuid == destination.uuid }
                if (player != null) {
                    return if (player.instance == holder.instance) player.position else null
                }

                val tracked = activeByEntityId[destination.uuid]?.entity ?: return null
                if (tracked.instance != holder.instance) return null
                tracked.position
            }
        }
    }
}


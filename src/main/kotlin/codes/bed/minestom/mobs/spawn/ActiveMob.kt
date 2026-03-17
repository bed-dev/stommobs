package codes.bed.minestom.mobs.spawn

import net.minestom.server.entity.EntityCreature
import codes.bed.minestom.mobs.api.MobProfile

data class ActiveMob(
    val profile: MobProfile,
    val entity: EntityCreature,
    val level: Int,
    val spawnedAtTick: Long,
    var ladderClimbOverride: Boolean? = null,
    var followDestination: FollowDestination? = null,
    var followMinDistance: Double = 1.5,
    var followRefreshIntervalTicks: Int = 8,
    var lastFollowTick: Long = 0,
    var lastAdaptiveRepathTick: Long = 0,
    var stationaryTicks: Int = 0,
    var lastNavigationPos: net.minestom.server.coordinate.Pos? = null
)


import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import codes.bed.minestom.mobs.StomMobs
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.dsl.mobProfile
import codes.bed.minestom.mobs.dsl.spawnDsl
import codes.bed.minestom.mobs.dsl.spawnMob

fun kotlinDslExample(node: EventNode<Event>, instance: Instance) {
    val spawner = StomMobs.spawnerWithVanillaProfiles(node)

    spawner.spawnMob("vanilla:sheep", instance, Pos(0.0, 42.0, 0.0), level = 3)

    val guardian = mobProfile("custom:guardian", EntityType.DROWNED) {
        stats(
            MobStats(
                baseHealth = 34.0,
                healthPerLevel = 5.0,
                baseAttack = 7.0,
                attackPerLevel = 1.2,
                baseMoveSpeed = 0.24,
                speedPerLevel = 0.008,
                baseArmor = 5.0,
                armorPerLevel = 0.4,
                baseFollowRange = 30.0,
                followRangePerLevel = 0.2
            )
        )
        pathing(PathingPresets.hostileMelee(speed = 1.7, attackDelay = 14, searchRange = 30.0))
    }

    spawner.register(guardian)

    spawner.spawnDsl {
        profile = guardian
        this.instance = instance
        position = Pos(5.0, 42.0, 5.0)
        level = 6
    }
}


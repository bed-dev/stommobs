import net.minestom.server.coordinate.Pos
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import codes.bed.minestom.mobs.StomMobs

fun vanillaQuickSpawnExample(node: EventNode<Event>, instance: Instance) {
    val spawner = StomMobs.spawnerWithVanillaProfiles(node)

    spawner.spawnAt("vanilla:zombie", instance, 0.0, 42.0, 0.0, level = 6)
    spawner.spawnAt("vanilla:villager", instance, 4.0, 42.0, 4.0, level = 2)
    spawner.spawnAt("vanilla:sheep", instance, 8.0, 42.0, 8.0, level = 2)
}


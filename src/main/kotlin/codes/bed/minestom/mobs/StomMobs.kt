package codes.bed.minestom.mobs

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import codes.bed.minestom.mobs.profile.vanilla.VanillaProfiles
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.spawn.MobSpawner

/**
 * Library entrypoint helpers for creating preconfigured spawners.
 */
object StomMobs {

    /** Alias for [emptySpawner] for API readability. */
    @JvmStatic
    fun createSpawner(node: EventNode<Event>): MobSpawner = emptySpawner(node)

    /** Creates a spawner bound to [node] without registering profiles. */
    @JvmStatic
    fun emptySpawner(node: EventNode<Event>): MobSpawner {
        val spawner = MobSpawner()
        spawner.bind(node)
        return spawner
    }

    /** Registers the built-in vanilla profiles into an existing [spawner]. */
    @JvmStatic
    fun registerVanillaProfiles(spawner: MobSpawner): MobSpawner {
        return spawner.registerAll(VanillaProfiles.all())
    }

    /** Registers custom profiles into an existing [spawner]. */
    @JvmStatic
    fun registerProfiles(spawner: MobSpawner, profiles: Iterable<MobProfile>): MobSpawner {
        return spawner.registerAll(profiles)
    }

    /** Returns all built-in vanilla profile definitions. */
    @JvmStatic
    fun vanillaProfiles(): List<MobProfile> = VanillaProfiles.all()

    /** Returns a single built-in vanilla profile by id, if present. */
    @JvmStatic
    fun vanillaProfile(id: String): MobProfile? = VanillaProfiles.all().firstOrNull { profile -> profile.id == id }

    /** Creates and binds an empty spawner or a vanilla-preloaded spawner. */
    @JvmStatic
    fun createSpawner(node: EventNode<Event>, withVanillaProfiles: Boolean): MobSpawner {
        return if (withVanillaProfiles) spawnerWithVanillaProfiles(node) else emptySpawner(node)
    }

    /** Creates and binds a spawner, then registers custom profiles. */
    @JvmStatic
    fun spawnerWithProfiles(node: EventNode<Event>, profiles: Iterable<MobProfile>): MobSpawner {
        val spawner = emptySpawner(node)
        spawner.registerAll(profiles)
        return spawner
    }

    /**
     * Creates a [MobSpawner], registers all built-in vanilla profiles,
     * and binds mob lifecycle listeners to the provided [node].
     */
    @JvmStatic
    fun spawnerWithVanillaProfiles(node: EventNode<Event>): MobSpawner {
        val spawner = MobSpawner()
            .registerAll(VanillaProfiles.all())
        spawner.bind(node)
        return spawner
    }
}



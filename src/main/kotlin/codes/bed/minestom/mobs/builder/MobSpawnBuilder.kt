package codes.bed.minestom.mobs.builder

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.instance.Instance
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.spawn.MobSpawner

/**
 * Java-friendly builder for spawning mobs from a [MobSpawner].
 */
class MobSpawnBuilder private constructor(
    private val spawner: MobSpawner
) {
    private var profileId: String? = null
    private var profile: MobProfile? = null
    private var instance: Instance? = null
    private var position: Pos? = null
    private var level: Int = 1

    /** Spawns by registered profile id. */
    fun profileId(profileId: String): MobSpawnBuilder = apply {
        this.profileId = profileId
        this.profile = null
    }

    /** Spawns using a direct profile object. */
    fun profile(profile: MobProfile): MobSpawnBuilder = apply {
        this.profile = profile
        this.profileId = null
    }

    /** Sets target instance. */
    fun instance(instance: Instance): MobSpawnBuilder = apply { this.instance = instance }

    /** Sets target position. */
    fun position(position: Pos): MobSpawnBuilder = apply { this.position = position }

    /** Sets spawn level. Values lower than 1 are normalized to 1. */
    fun level(level: Int): MobSpawnBuilder = apply {
        this.level = if (level < 1) 1 else level
    }

    /** Performs the spawn operation using the configured inputs. */
    fun spawn(): EntityCreature {
        val finalInstance = requireNotNull(instance) { "Mob spawn requires an Instance." }
        val finalPosition = requireNotNull(position) { "Mob spawn requires a Pos." }

        val directProfile = profile
        if (directProfile != null) {
            return spawner.spawn(directProfile, finalInstance, finalPosition, level)
        }

        val finalProfileId = requireNotNull(profileId) {
            "Mob spawn requires either profile() or profileId()."
        }
        return spawner.spawn(finalProfileId, finalInstance, finalPosition, level)
    }

    companion object {
        /** Creates a new spawn builder bound to the given spawner. */
        @JvmStatic
        fun create(spawner: MobSpawner): MobSpawnBuilder = MobSpawnBuilder(spawner)
    }
}



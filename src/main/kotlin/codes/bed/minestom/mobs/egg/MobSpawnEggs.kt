package codes.bed.minestom.mobs.egg

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import codes.bed.minestom.mobs.spawn.MobSpawner
import kotlin.math.max

/**
 * Spawn egg helpers for stommobs profiles.
 */
object MobSpawnEggs {
    private val profileIdTag: Tag<String> = Tag.String("stommobs_profile_id")
    private val levelTag: Tag<Int> = Tag.Integer("stommobs_level")

    @JvmStatic
    fun create(profileId: String, level: Int = 1): ItemStack {
        val normalizedLevel = max(1, level)
        val material = eggMaterial(profileId)
        return ItemStack.builder(material)
            .set(profileIdTag, profileId)
            .set(levelTag, normalizedLevel)
            .customName(Component.text("Spawn Egg: $profileId (Lv$normalizedLevel)"))
            .build()
    }

    @JvmStatic
    fun isEgg(itemStack: ItemStack): Boolean = itemStack.getTag(profileIdTag) != null

    @JvmStatic
    fun profileId(itemStack: ItemStack): String? = itemStack.getTag(profileIdTag)

    @JvmStatic
    fun level(itemStack: ItemStack): Int = max(1, itemStack.getTag(levelTag) ?: 1)

    @JvmStatic
    fun giveAll(player: Player, spawner: MobSpawner, level: Int = 1) {
        spawner.registeredProfileIds().forEach { id ->
            player.inventory.addItemStack(create(id, level))
        }
    }

    @JvmStatic
    fun trySpawnFromEgg(itemStack: ItemStack, player: Player, instance: Instance, spawner: MobSpawner): Boolean {
        val profileId = profileId(itemStack) ?: return false
        val spawnPos = player.position.add(0.0, 0.0, 2.0)
        spawner.spawn(profileId, instance, spawnPos, level(itemStack))
        return true
    }

    @JvmStatic
    fun trySpawnFromEggAt(itemStack: ItemStack, instance: Instance, spawnPos: Pos, spawner: MobSpawner): Boolean {
        val profileId = profileId(itemStack) ?: return false
        spawner.spawn(profileId, instance, spawnPos, level(itemStack))
        return true
    }

    private fun eggMaterial(profileId: String): Material {
        val key = profileId.substringAfter(':', profileId).lowercase()
        return when {
            key.contains("zombie") -> Material.ZOMBIE_SPAWN_EGG
            key.contains("villager") -> Material.VILLAGER_SPAWN_EGG
            key.contains("sheep") -> Material.SHEEP_SPAWN_EGG
            else -> Material.SLIME_SPAWN_EGG
        }
    }
}


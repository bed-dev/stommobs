package codes.bed.minestom.mobs.profile.vanilla

import codes.bed.minestom.mobs.api.MobProfile

object VanillaProfiles {
    @JvmStatic
    fun zombie(): MobProfile = ZombieProfile.create()

    @JvmStatic
    fun villager(): MobProfile = VillagerProfile.create()

    @JvmStatic
    fun sheep(): MobProfile = SheepProfile.create()

    @JvmStatic
    fun wolf(): MobProfile = WolfProfile.create()

    @JvmStatic
    fun ironGolem(): MobProfile = IronGolemProfile.create()

    @JvmStatic
    fun enderman(): MobProfile = EndermanProfile.create()

    @JvmStatic
    fun all(): List<MobProfile> = listOf(
        zombie(),
        villager(),
        sheep(),
        wolf(),
        ironGolem(),
        enderman()
    )
}


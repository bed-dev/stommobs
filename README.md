# stommobs

`stommobs` is a focused Minestom library for **mob spawning** and **AI management**.
It gives you:

- A clean `MobSpawner` runtime with profile registration and active tracking
- Reusable AI/pathfinding presets built on Minestom goal/target selectors
- Java-friendly Builders for custom profiles and spawn requests
- Kotlin DSLs for concise custom AI assembly and spawning
- Ready-to-use vanilla-like profiles for Zombie, Villager, and Sheep

## Installation (JitPack)

### Gradle Kotlin DSL

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.bed-dev:stommobs:<version>")
}
```

### Gradle Groovy DSL

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.bed-dev:stommobs:<version>'
}
```

## Java Example (Builders)

```java
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import codes.bed.minestom.mobs.StomMobs;
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets;
import codes.bed.minestom.mobs.api.MobStats;
import codes.bed.minestom.mobs.builder.MobProfileBuilder;
import codes.bed.minestom.mobs.builder.MobSpawnBuilder;
import codes.bed.minestom.mobs.profile.vanilla.VanillaProfiles;
import codes.bed.minestom.mobs.spawn.MobSpawner;

public final class JavaMobExample {
    public static void spawnMobs(EventNode<Event> node, Instance instance) {
        MobSpawner spawner = StomMobs.spawnerWithVanillaProfiles(node);

        // Spawn pre-made zombie profile
        MobSpawnBuilder.create(spawner)
            .profileId("vanilla:zombie")
            .instance(instance)
            .position(new Pos(0, 42, 0))
            .level(8)
            .spawn();

        // Build + register a custom hostile mob profile
        var custom = MobProfileBuilder.create("custom:raider", EntityType.HUSK)
            .stats(new MobStats(
                28.0, 4.5,
                6.0, 1.4,
                0.26, 0.012,
                3.0, 0.25,
                28.0, 0.25
            ))
            .applyPathing(PathingPresets.hostileMelee(1.65, 16, 30.0, true, entity -> true))
            .build();

        spawner.register(custom);

        MobSpawnBuilder.create(spawner)
            .profile(custom)
            .instance(instance)
            .position(new Pos(6, 42, 6))
            .level(5)
            .spawn();

        // Spawn pre-made villager and sheep
        spawner.register(VanillaProfiles.villager()).register(VanillaProfiles.sheep());
        spawner.spawn("vanilla:villager", instance, new Pos(8, 42, 8), 2);
        spawner.spawn("vanilla:sheep", instance, new Pos(10, 42, 10), 2);
    }
}
```

## Kotlin Example (DSL)

```kotlin
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import codes.bed.minestom.mobs.StomMobs
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobStats
import codes.bed.minestom.mobs.dsl.mobProfile
import codes.bed.minestom.mobs.dsl.spawnMob

fun spawnWithDsl(node: EventNode<Event>, instance: Instance) {
    val spawner = StomMobs.spawnerWithVanillaProfiles(node)

    // Spawn a pre-made vanilla zombie
    spawner.spawnMob {
        profileId = "vanilla:zombie"
        this.instance = instance
        position = Pos(0.0, 42.0, 0.0)
        level = 10
    }

    // Assemble a custom mob profile with DSL
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
        pathing(
            PathingPresets.hostileMelee(
                speed = 1.7,
                attackDelay = 14,
                searchRange = 30.0
            )
        )
    }

    spawner.register(guardian)

    spawner.spawnMob {
        profile = guardian
        this.instance = instance
        position = Pos(5.0, 42.0, 5.0)
        level = 6
    }
}
```

## Included Vanilla Profiles

- `vanilla:zombie`
- `vanilla:villager`
- `vanilla:sheep`
- `vanilla:wolf` (hunter AI)
- `vanilla:iron_golem` (defender AI)
- `vanilla:enderman` (ambusher AI)

## Documentation

Central docs are published at **https://stom.bed.codes**.

- StomMobs docs: https://stom.bed.codes/stommobs/
- Introduction: https://stom.bed.codes/stommobs/introduction
- Installation: https://stom.bed.codes/stommobs/getting-started/installation
- Quick Start: https://stom.bed.codes/stommobs/getting-started/quickstart
- API Overview: https://stom.bed.codes/stommobs/api/overview

## Examples

- `examples/README.md`
- `examples/java/JavaBuildersExample.java`
- `examples/kotlin/KotlinDslExample.kt`
- `examples/kotlin/VanillaQuickSpawnExample.kt`

## Spawn Eggs API

`codes.bed.minestom.mobs.egg.MobSpawnEggs` lets you create and distribute profile-based spawn eggs.

```kotlin
MobSpawnEggs.giveAll(player, spawner, level = 1)
```

```kotlin
MobSpawnEggs.trySpawnFromEgg(itemStack, player, instance, spawner)
```


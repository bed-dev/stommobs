import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import codes.bed.minestom.mobs.StomMobs;
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets;
import codes.bed.minestom.mobs.api.MobStats;
import codes.bed.minestom.mobs.builder.MobProfileBuilder;
import codes.bed.minestom.mobs.builder.MobSpawnBuilder;
import codes.bed.minestom.mobs.spawn.MobSpawner;

public final class JavaBuildersExample {
    private JavaBuildersExample() {
    }

    public static void run(EventNode<Event> node, Instance instance, Player player) {
        MobSpawner spawner = StomMobs.spawnerWithVanillaProfiles(node);

        var zombie = MobSpawnBuilder.create(spawner)
            .profileId("vanilla:zombie")
            .instance(instance)
            .position(new Pos(0, 42, 0))
            .level(8)
            .spawn();

        spawner.spawnAt("vanilla:villager", instance, 4, 42, 4, 2);
        spawner.spawnAt("vanilla:sheep", instance, 8, 42, 8, 2);

        var custom = MobProfileBuilder.create("custom:raider", EntityType.HUSK)
            .stats(new MobStats(
                28.0, 4.5,
                6.0, 1.4,
                0.26, 0.012,
                3.0, 0.25,
                30.0, 0.25
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

        spawner.followPlayer(zombie, player);
    }
}


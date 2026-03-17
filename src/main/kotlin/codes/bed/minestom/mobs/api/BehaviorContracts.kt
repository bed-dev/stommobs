package codes.bed.minestom.mobs.api

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.entity.ai.TargetSelector
import java.util.function.Predicate

/** Builds a Minestom goal selector for a concrete spawned creature. */
fun interface GoalFactory {
    fun create(creature: EntityCreature): GoalSelector
}

/** Builds a Minestom target selector for a concrete spawned creature. */
fun interface TargetFactory {
    fun create(creature: EntityCreature): TargetSelector
}

/** Handles mob lifecycle events such as spawn, tick, attack and death. */
fun interface MobLifecycleHook {
    fun handle(context: MobContext)
}

/** Java-friendly target predicate wrapper used by higher-level APIs. */
fun interface TargetPredicate {
    fun test(entity: Entity): Boolean

    companion object {
        /** Converts a standard Java predicate into a [TargetPredicate]. */
        @JvmStatic
        fun from(predicate: Predicate<Entity>): TargetPredicate = TargetPredicate { entity ->
            predicate.test(entity)
        }
    }
}



package codes.bed.minestom.mobs.spawn

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag
import codes.bed.minestom.mobs.ai.pathfinding.HazardAwareGroundNodeGenerator
import codes.bed.minestom.mobs.ai.pathfinding.HazardPolicy
import codes.bed.minestom.mobs.ai.pathfinding.LadderAwareGroundNodeFollower
import codes.bed.minestom.mobs.ai.pathfinding.PathTraversalOptions
import codes.bed.minestom.mobs.ai.pathfinding.PathingPresets
import codes.bed.minestom.mobs.api.MobContext
import codes.bed.minestom.mobs.api.MobProfile
import codes.bed.minestom.mobs.builder.MobSpawnBuilder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * Runtime registry and spawn service for [MobProfile] instances.
 */
class MobSpawner {
    private val noHazardPolicy = HazardPolicy { false }
    private val tickCounter = AtomicLong(0)
    private val profilesById = ConcurrentHashMap<String, MobProfile>()
    private val activeByEntityId = ConcurrentHashMap<UUID, ActiveMob>()
    private val destinationResolver = FollowDestinationResolver(activeByEntityId)
    private val ladderAssist = LadderAssistService(::distanceSquared)

    /** Registers or replaces a single profile by id. */
    fun register(profile: MobProfile): MobSpawner = apply {
        require(profile.id.isNotBlank()) { "Mob profile id cannot be blank." }
        profilesById[profile.id] = profile
    }

    /** Registers all profiles from an iterable collection. */
    fun registerAll(profiles: Iterable<MobProfile>): MobSpawner = apply {
        profiles.forEach(::register)
    }

    /** Looks up a registered profile by id. */
    fun getProfile(id: String): MobProfile? = profilesById[id]

    /** Returns a snapshot of all registered profile ids. */
    fun registeredProfileIds(): List<String> = profilesById.keys.toList().sorted()

    /** Returns a snapshot of all registered profiles. */
    fun registeredProfiles(): List<MobProfile> = profilesById.values.toList()

    /** Returns a snapshot of currently tracked active mobs. */
    fun getActiveMobs(): List<ActiveMob> = activeByEntityId.values.toList()

    /** Creates a Java-friendly spawn builder bound to this spawner. */
    fun spawnBuilder(): MobSpawnBuilder = MobSpawnBuilder.create(this)

    /** Sets a block destination that the mob will keep following automatically. */
    fun setDestinationBlock(
        creature: EntityCreature,
        destination: Pos,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.5
    ): Boolean = updateFollow(creature.uuid) { tracked ->
        tracked.followDestination = FollowDestination.Block(destination)
        tracked.followRefreshIntervalTicks = refreshIntervalTicks.coerceAtLeast(1)
        tracked.followMinDistance = minDistance.coerceAtLeast(0.2)
        tracked.entity.target = null
    }

    /** UUID overload for Java/Kotlin callers that only keep entity ids. */
    fun setDestinationBlock(
        entityUuid: UUID,
        destination: Pos,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.5
    ): Boolean = updateFollow(entityUuid) { tracked ->
        tracked.followDestination = FollowDestination.Block(destination)
        tracked.followRefreshIntervalTicks = refreshIntervalTicks.coerceAtLeast(1)
        tracked.followMinDistance = minDistance.coerceAtLeast(0.2)
        tracked.entity.target = null
    }

    /** Sets an entity destination that the mob will keep following automatically. */
    fun setEntityDestination(
        creature: EntityCreature,
        target: Entity,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.8
    ): Boolean = setEntityDestination(creature.uuid, target.uuid, refreshIntervalTicks, minDistance)

    /** UUID overload for dynamic entity-tracking destination. */
    fun setEntityDestination(
        entityUuid: UUID,
        targetEntityUuid: UUID,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.8
    ): Boolean = updateFollow(entityUuid) { tracked ->
        tracked.followDestination = FollowDestination.Entity(targetEntityUuid)
        tracked.followRefreshIntervalTicks = refreshIntervalTicks.coerceAtLeast(1)
        tracked.followMinDistance = minDistance.coerceAtLeast(0.2)
        tracked.entity.target = null
    }

    /** Sets a player destination that the mob will keep following automatically. */
    fun setPlayerDestination(
        creature: EntityCreature,
        player: Player,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.8
    ): Boolean = setEntityDestination(creature.uuid, player.uuid, refreshIntervalTicks, minDistance)

    /** UUID overload for dynamic player-tracking destination. */
    fun setPlayerDestination(
        entityUuid: UUID,
        playerUuid: UUID,
        refreshIntervalTicks: Int = 1,
        minDistance: Double = 1.8
    ): Boolean = setEntityDestination(entityUuid, playerUuid, refreshIntervalTicks, minDistance)

    /** Clears destination-follow behavior for the given mob. */
    fun clearDestination(creature: EntityCreature): Boolean = clearDestination(creature.uuid)

    /** Clears destination-follow behavior for the given mob UUID. */
    fun clearDestination(entityUuid: UUID): Boolean = updateFollow(entityUuid) { tracked ->
        tracked.followDestination = null
    }

    /** Enables/disables ladder climbing for an active mob at runtime. */
    fun setLadderClimbing(creature: EntityCreature, enabled: Boolean): Boolean = setLadderClimbing(creature.uuid, enabled)

    /** UUID overload to enable/disable ladder climbing for an active mob at runtime. */
    fun setLadderClimbing(entityUuid: UUID, enabled: Boolean): Boolean {
        val tracked = activeByEntityId[entityUuid] ?: return false
        tracked.ladderClimbOverride = enabled
        configureNavigator(
            creature = tracked.entity,
            traversal = tracked.profile.traversalOptions,
            allowLadders = enabled
        )
        return true
    }

    /** Spawns an entity from a registered profile id. */
    fun spawn(profileId: String, instance: Instance, position: Pos, level: Int = 1): EntityCreature {
        val profile = profilesById[profileId]
            ?: error("Unknown mob profile id '$profileId'. Register the profile before spawning.")
        return spawn(profile = profile, instance = instance, position = position, level = level)
    }

    /** Spawns an entity from a direct profile object. */
    fun spawn(profile: MobProfile, instance: Instance, position: Pos, level: Int = 1): EntityCreature {
        val creature = EntityCreature(profile.entityType)
        profile.stats.applyTo(creature = creature, level = level)

        val goals = profile.goals.map { it.create(creature) }
        val targets = profile.targets.map { it.create(creature) }
        creature.addAIGroup(goals, targets)
        configureNavigator(
            creature = creature,
            traversal = profile.traversalOptions,
            allowLadders = profile.traversalOptions.allowLadders
        )

        creature.setInstance(instance, position)
        val active = ActiveMob(
            profile = profile,
            entity = creature,
            level = level,
            spawnedAtTick = tickCounter.get()
        )
        activeByEntityId[creature.uuid] = active

        profile.onSpawn.handle(
            MobContext(
                spawner = this,
                profile = profile,
                entity = creature,
                level = level,
                tick = tickCounter.get()
            )
        )
        return creature
    }

    /** Stops tracking an active mob entity by UUID. */
    fun unregister(creature: EntityCreature): Boolean {
        return activeByEntityId.remove(creature.uuid) != null
    }

    /** Binds mob lifecycle listeners onto the given Minestom event node. */
    fun bind(node: EventNode<Event>) {
        node.addListener(EntityTickEvent::class.java) { event ->
            val tracked = activeByEntityId[event.entity.uuid] ?: return@addListener
            tickCounter.incrementAndGet()
            applyDestinationFollow(tracked)
            applyMovementAssist(tracked)
            tracked.profile.onTick.handle(
                MobContext(
                    spawner = this,
                    profile = tracked.profile,
                    entity = tracked.entity,
                    level = tracked.level,
                    tick = tickCounter.get()
                )
            )
            applySmoothLook(tracked)
            applyAdaptiveRepath(tracked)
        }

        node.addListener(EntityAttackEvent::class.java) { event ->
            val tracked = activeByEntityId[event.entity.uuid] ?: return@addListener
            tracked.profile.onAttack.handle(
                MobContext(
                    spawner = this,
                    profile = tracked.profile,
                    entity = tracked.entity,
                    level = tracked.level,
                    tick = tickCounter.get()
                )
            )
        }

        node.addListener(EntityDeathEvent::class.java) { event ->
            val tracked = activeByEntityId.remove(event.entity.uuid) ?: return@addListener
            tracked.profile.onDeath.handle(
                MobContext(
                    spawner = this,
                    profile = tracked.profile,
                    entity = tracked.entity,
                    level = tracked.level,
                    tick = tickCounter.get()
                )
            )
        }
    }

    private fun updateFollow(entityUuid: UUID, action: (ActiveMob) -> Unit): Boolean {
        val tracked = activeByEntityId[entityUuid] ?: return false
        action(tracked)
        
        // Reset lastFollowTick to ensure immediate path update in next tick loop
        tracked.lastFollowTick = 0
        
        if (tracked.followDestination != null) {
            tracked.entity.setTag(Tag.Boolean("stommobs:manual_move"), true)
        } else {
            tracked.entity.removeTag(Tag.Boolean("stommobs:manual_move"))
        }
        return true
    }

    private fun applyDestinationFollow(tracked: ActiveMob) {
        val destination = tracked.followDestination ?: return

        val entityTicks = tracked.entity.aliveTicks
        if (entityTicks - tracked.lastFollowTick < tracked.followRefreshIntervalTicks) return

        tracked.entity.target = null

        val targetPos = destinationResolver.resolvePosition(tracked.entity, destination) ?: return
        
        // Let applySmoothLook handle rotation consistently.
        if (distanceSquared(tracked.entity.position, targetPos) <= tracked.followMinDistance * tracked.followMinDistance) {
            tracked.lastFollowTick = entityTicks
            return
        }

        if (resolveAllowLadders(tracked) && ladderAssist.tryLadderAssist(tracked.entity, targetPos)) {
            tracked.lastFollowTick = entityTicks
            return
        }

        val pathSet = tracked.entity.navigator.setPathTo(targetPos)
        if (!pathSet) {
            val fallback = findNearestReachableFollowPoint(tracked.entity, targetPos)
            if (fallback != null) {
                tracked.entity.navigator.setPathTo(fallback)
            }
        }
        tracked.lastFollowTick = entityTicks
    }

    private fun distanceSquared(from: Point, to: Point): Double {
        val dx = from.x() - to.x()
        val dy = from.y() - to.y()
        val dz = from.z() - to.z()
        return (dx * dx) + (dy * dy) + (dz * dz)
    }

    private fun resolveHazardPolicy(entity: EntityCreature, traversal: PathTraversalOptions): HazardPolicy {
        val avoidHazards = if (entity.target == null) {
            traversal.avoidHazardsWhenCalm
        } else {
            traversal.avoidHazardsWhenAggressive
        }
        return if (avoidHazards) PathingPresets.defaultHazardPolicy else noHazardPolicy
    }

    private fun resolveAllowLadders(tracked: ActiveMob): Boolean {
        return tracked.ladderClimbOverride ?: tracked.profile.traversalOptions.allowLadders
    }

    private fun configureNavigator(creature: EntityCreature, traversal: PathTraversalOptions, allowLadders: Boolean) {
        creature.navigator.setNodeFollower {
            LadderAwareGroundNodeFollower(
                entity = creature,
                allowLadders = allowLadders,
                allowSwimming = traversal.allowSwimming
            )
        }
        creature.navigator.setNodeGenerator {
            HazardAwareGroundNodeGenerator(
                policyProvider = { resolveHazardPolicy(creature, traversal) },
                allowLadders = allowLadders,
                allowSwimming = traversal.allowSwimming,
                maxDropHeight = traversal.maxDropHeight
            )
        }
    }

    private fun findNearestReachableFollowPoint(entity: EntityCreature, target: Point): Pos? {
        val navigator = entity.navigator
        val candidates = mutableListOf<Pos>()

        // Prefer blocks directly below the player (handles flying/unreachable targets).
        for (down in 1..12) {
            candidates += Pos(target.x(), target.y() - down, target.z())
        }

        // Expand around the player to find the nearest reachable edge/adjacent block.
        for (radius in 1..6) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dz) != radius) continue
                    for (down in 0..8) {
                        candidates += Pos(target.x() + dx, target.y() - down, target.z() + dz)
                    }
                }
            }
        }

        val uniqueCandidates = candidates
            .distinctBy { pos -> "${pos.blockX()}:${pos.blockY()}:${pos.blockZ()}" }
            .sortedBy { pos -> distanceSquared(entity.position, pos) }

        for (candidate in uniqueCandidates) {
            if (navigator.setPathTo(candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun applySmoothLook(tracked: ActiveMob) {
        val entity = tracked.entity
        val destination = tracked.followDestination
        if (destination != null) {
            val targetPos = destinationResolver.resolveLookPosition(entity, destination) ?: return
            SmoothLookController.smoothLookAt(entity, targetPos)
            return
        }

        val target = entity.target
        if (target != null) {
            val lookTarget = target.position.add(0.0, target.eyeHeight, 0.0)
            SmoothLookController.smoothLookAt(entity, lookTarget)
            return
        }

        val goal = entity.navigator.goalPosition
        if (goal != null) {
            SmoothLookController.smoothLookAt(entity, goal.add(0.0, entity.eyeHeight, 0.0))
        }
    }

    private fun applyMovementAssist(tracked: ActiveMob) {
        val entity = tracked.entity
        val currentTarget = entity.target
        val targetPos = when {
            tracked.followDestination != null -> {
                destinationResolver.resolvePosition(entity, tracked.followDestination!!)
            }
            currentTarget != null -> currentTarget.position
            else -> entity.navigator.goalPosition
        } ?: return

        if (resolveAllowLadders(tracked) && ladderAssist.tryLadderAssist(entity, targetPos)) {
            return
        }

        tryJumpForward(entity, targetPos)
    }

    private fun applyAdaptiveRepath(tracked: ActiveMob) {
        val entity = tracked.entity
        val goal = entity.navigator.goalPosition ?: run {
            tracked.stationaryTicks = 0
            tracked.lastNavigationPos = null
            return
        }

        val currentPos = entity.position
        val previous = tracked.lastNavigationPos
        tracked.lastNavigationPos = currentPos
        if (previous == null) return

        val movedSq = distanceSquared(previous, currentPos)
        val goalDistanceSq = distanceSquared(currentPos, goal)

        if (goalDistanceSq > 4.0 && movedSq < 0.0015) {
            tracked.stationaryTicks += 1
        } else {
            tracked.stationaryTicks = 0
        }

        val ticks = entity.aliveTicks
        val shouldForceRepath = tracked.stationaryTicks >= 8 || (ticks - tracked.lastAdaptiveRepathTick) >= 20
        if (shouldForceRepath) {
            entity.navigator.setPathTo(goal)
            tracked.lastAdaptiveRepathTick = ticks
            if (tracked.stationaryTicks >= 8) tracked.stationaryTicks = 0
        }
    }

    private fun tryJumpForward(entity: EntityCreature, target: Point) {
        val instance = entity.instance ?: return
        val pos = entity.position
        val dx = target.x() - pos.x()
        val dz = target.z() - pos.z()
        val horizontal = sqrt((dx * dx) + (dz * dz))
        if (horizontal < 0.1) return

        val nx = dx / horizontal
        val nz = dz / horizontal
        val checkX = pos.x() + (nx * 0.8)
        val checkZ = pos.z() + (nz * 0.8)

        val feetY = pos.blockY()
        val bx = checkX.toInt()
        val bz = checkZ.toInt()
        val frontFeet = instance.getBlock(bx, feetY, bz)
        val frontHead = instance.getBlock(bx, feetY + 1, bz)
        val aboveHead = instance.getBlock(bx, feetY + 2, bz)

        val obstacle = !frontFeet.isAir && !isOpenBottomTrapdoor(frontFeet)
        if (!obstacle) return
        if (!frontHead.isAir || !aboveHead.isAir) return

        if (entity.velocity.y() > 0.05) return
        entity.velocity = Vec(entity.velocity.x() * 0.75, 0.42, entity.velocity.z() * 0.75)
    }

    private fun isOpenBottomTrapdoor(block: net.minestom.server.instance.block.Block): Boolean {
        val name = block.name().lowercase()
        if (!name.contains("trapdoor")) return false
        val half = block.getProperty("half") ?: return false
        val open = block.getProperty("open")?.equals("true", ignoreCase = true) == true
        return half.equals("bottom", ignoreCase = true) && open
    }
}

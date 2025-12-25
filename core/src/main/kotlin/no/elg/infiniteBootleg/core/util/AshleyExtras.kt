package no.elg.infiniteBootleg.core.util

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.EntityFlags.INVALID_FLAG
import no.elg.infiniteBootleg.core.util.EntityFlags.hasFlag
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason

class EntityAddListener(private val addListener: (Entity) -> Unit) : EntityListener {
  override fun entityAdded(entity: Entity) = addListener(entity)
  override fun entityRemoved(entity: Entity?) = Unit
}

class EntityRemoveListener(private val removeListener: (Entity) -> Unit) : EntityListener {
  override fun entityAdded(entity: Entity?) = Unit
  override fun entityRemoved(entity: Entity) = removeListener(entity)
}

fun Entity.interactableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<WorldCompactLoc> {
  val (worldX, worldY) = this.position
  return World.getLocationsWithin(centerBlockX, centerBlockY, radius).asSequence()
    .filter { worldLoc: WorldCompactLoc -> world.isChunkLoaded(worldLoc.worldToChunk()) }
    .filter { (targetX, targetY) ->
      ignorePlaceableCheck ||
        (
          isBlockInsideRadius(worldX, worldY, targetX, targetY, interactionRadius) &&
            (!Settings.renderLight || world.getBlockLight(targetX, targetY, false)?.isLit ?: true)
          )
    }
}

fun Entity.breakableLocs(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<WorldCompactLoc> = interactableBlocks(world, centerBlockX, centerBlockY, radius, interactionRadius).filterNot { world.isAirBlock(it, false) }

fun Entity.placeableBlocks(world: World, centerBlockX: WorldCoord, centerBlockY: WorldCoord, interactionRadius: Float): Sequence<Long> =
  interactableBlocks(world, centerBlockX, centerBlockY, 1f, interactionRadius)
    .filter { world.isAirBlock(it) }
    .let { seq ->
      if (seq.any { (worldX, worldY) -> world.canEntityPlaceBlock(worldX, worldY, this) }) {
        seq
      } else {
        emptySequence()
      }
    }

fun Entity.toComponentsString() = "${components.filterNotNull().map { it.javaClass.simpleName.removeSuffix("Component") }.sorted()}"

val Entity.isBeingRemoved: Boolean get() = isRemoving || isScheduledForRemoval || hasFlag(INVALID_FLAG)

fun Entity.removeSelf(reason: DespawnReason = DespawnReason.UNKNOWN_REASON) = this.world.removeEntity(this, reason)

typealias EntityFlag = Int

object EntityFlags {

  /**
   * This entity is scheduled for removal, or has been removed.
   *
   * Marks the entity as not valid anymore
   */
  const val INVALID_FLAG: EntityFlag = Int.MIN_VALUE

  fun Entity.hasFlag(flag: EntityFlag): Boolean = flags and flag != 0
  fun Entity.enableFlag(flag: EntityFlag) {
    flags = flags or flag
  }

  fun Entity.disableFlag(flag: EntityFlag) {
    flags = flags and flag.inv()
  }
}

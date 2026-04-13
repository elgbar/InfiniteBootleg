package no.elg.infiniteBootleg.core.util

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.EntityFlags.INVALID_FLAG
import no.elg.infiniteBootleg.core.util.EntityFlags.hasFlag
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
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

/**
 * @param interactionRadius How far the entity can reach
 */
fun Entity.interactableBlocksWithinRadius(world: World, interactionRadius: Float, baseSeq: Sequence<WorldCompactLoc>): Sequence<WorldCompactLoc> {
  val (worldX, worldY) = positionComponent
  return baseSeq.filter { worldLoc: WorldCompactLoc -> world.isChunkLoaded(worldLoc.worldToChunk()) }.filter { (targetX, targetY) ->
    ignorePlaceableCheck || (
      isBlockInsideRadius(worldX, worldY, targetX, targetY, interactionRadius) && (
        !Settings.renderLight || world.getBlockLight(
          targetX,
          targetY,
          false
        )?.isLit ?: true
        )
      )
  }
}

fun Entity.placeableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  interactionRadius: Float,
  material: Material
): Sequence<WorldCompactLoc> =
  interactableBlocksWithinRadius(world, interactionRadius, sequenceOf(compactInt(centerBlockX, centerBlockY))).filter { (worldX: WorldCoord, worldY: WorldCoord) ->
    world.isAirBlock(worldX, worldY, loadChunk = false) && material.canBeCreated(world, worldX, worldY, material)
  }.let { seq ->
    if (seq.any { (worldX, worldY) -> world.canEntityPlaceBlock(worldX, worldY, this) }) {
      seq
    } else {
      emptySequence()
    }
  }

val Class<out Component>.displayName: String get() = simpleName.removeSuffix("Component").removeSuffix("Tag")
val Component.displayName: String get() = javaClass.displayName

fun Entity.toComponentsString() = "${components.filterNotNull().map { it.displayName }.sorted()}"

val Entity.isInvalid: Boolean get() = isRemoving || isScheduledForRemoval || hasFlag(INVALID_FLAG)
val Entity.isValid: Boolean get() = !isInvalid

fun Entity.removeSelf(reason: DespawnReason = DespawnReason.UNKNOWN_REASON) = this.world.removeEntity(this, reason)

object EntityFlags {

  typealias EntityFlag = Int

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

package no.elg.infiniteBootleg.util

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.world.World

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
): Sequence<Long> {
  val pos = this.position
  return World.getLocationsWithin(centerBlockX, centerBlockY, radius).asSequence()
    .filter { worldLoc: WorldCompactLoc -> world.isChunkLoaded(worldLoc.worldToChunk()) }
    .filter { (worldX, worldY) ->
      ignorePlaceableCheck || (
        isBlockInsideRadius(pos.x, pos.y, worldX, worldY, interactionRadius) &&
          (!Settings.renderLight || world.getBlockLight(worldX, worldY, false)?.isLit ?: true)
        )
    }
}

fun Entity.breakableLocs(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  radius: Float,
  interactionRadius: Float
): Sequence<Long> {
  return interactableBlocks(world, centerBlockX, centerBlockY, radius, interactionRadius).filterNot { world.isAirBlock(it, false) }
}

fun Entity.placeableBlocks(
  world: World,
  centerBlockX: WorldCoord,
  centerBlockY: WorldCoord,
  interactionRadius: Float
): Sequence<Long> {
  return interactableBlocks(world, centerBlockX, centerBlockY, 1f, interactionRadius)
    .filter { world.isAirBlock(it) }
    .let {
      if (it.any { (worldX, worldY) -> world.canEntityPlaceBlock(worldX, worldY, this) }) {
        it
      } else {
        emptySequence()
      }
    }
}

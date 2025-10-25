package no.elg.infiniteBootleg.core.world.box2d.service

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.isBeingRemoved
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent.Companion.isType
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import org.jetbrains.annotations.Async

private val logger = KotlinLogging.logger {}

object AuthoritativeFallingBlockPhysicsEventHandler : PhysicsSystem.PhysicsEventHandler {

  private const val MAX_DELTA_UP = Chunk.CHUNK_SIZE

  fun handleFallingBlockContactBeginsEvent(entity: Entity) {
    if (entity.isBeingRemoved || !entity.isType(EntityType.FALLING_BLOCK)) {
      return
    }
    val material = entity.materialOrNull ?: return
    val positionComp = entity.positionComponent
    val newX: Int = positionComp.blockX
    val newY: Int = positionComp.blockY - 1
    val world = entity.world

    world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)

    var deltaY = 0
    do {
      if (deltaY > MAX_DELTA_UP) {
        logger.trace { "Reached max delta up for handling falling block contact" }
        return
      }
      deltaY++
      if (!world.isChunkLoaded(newX.worldToChunk(), (newY + deltaY).worldToChunk())) {
        logger.trace { "Reached a unloaded chunk, will not go further placing falling block" }
        return
      }
    } while (world.isNotAirBlock(newX, newY + deltaY))

    world.setBlock(newX, newY + deltaY, material)
  }

  override fun handleEvent(entity: Entity, @Async.Execute event: PhysicsEvent) {
    if (event is PhysicsEvent.ContactBeginsEvent) {
      handleFallingBlockContactBeginsEvent(entity)
    }
  }
}

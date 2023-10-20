package no.elg.infiniteBootleg.world.box2d.service

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData

object FallingBlockContactService {

  fun handleFallingBlockContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    if (Main.isAuthoritative) {
      val material = entity.materialOrNull ?: return
      val block = event.getOtherFixtureUserData<Block>(entity) { true } ?: return
      val newX: Int = block.worldX
      val newY: Int = block.worldY
      val world = block.world
      synchronized(world) {
        if (entity.isScheduledForRemoval || entity.isRemoving) {
          return
        }
        world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)

        var deltaY = 0
        var blockUp: Block?
        do {
          deltaY++
          blockUp = world.getBlock(newX, newY + deltaY, false)
        } while (blockUp?.material == material && !blockUp.isMarkerBlock())

        // Only place the block if there is actual free space above
        if (blockUp?.material == Material.AIR || blockUp.isMarkerBlock()) {
          world.setBlock(newX, newY + deltaY, material)
        }
      }
    }
  }
}

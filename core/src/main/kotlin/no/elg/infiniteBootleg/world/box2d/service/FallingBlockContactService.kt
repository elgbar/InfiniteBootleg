package no.elg.infiniteBootleg.world.box2d.service

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.main.Main
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
      var deltaY = 0
      val world = block.world
      synchronized(world) {
        if (entity.isScheduledForRemoval || entity.isRemoving) {
          return
        }
        var materialUp: Material

        do {
          deltaY++
          materialUp = world.getMaterial(newX, newY + deltaY)
        } while (materialUp == material)

        if (materialUp == Material.AIR) {
          world.setBlock(newX, newY + deltaY, material, true)
        }
        world.engine.removeEntity(entity)
      }
    }
  }
}

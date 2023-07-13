package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import ktx.ashley.remove
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData

object FallingBlockContactService {

  fun handleFallingBlockContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    if (Main.isAuthoritative()) {
      val material = entity.materialOrNull ?: return
      val block = event.getOtherFixtureUserData<Block>(entity) { true } ?: return
      val newX: Int = block.worldX
      val newY: Int = block.worldY
      var deltaY = 0
      val world = block.world
      if (entity.isScheduledForRemoval || entity.isRemoving) {
        return
      }
      synchronized(world) {
        var materialUp: Material

        do {
          deltaY++
          materialUp = world.getMaterial(newX, newY + deltaY)
        } while (materialUp == material)

        if (materialUp == Material.AIR) {
          world.setBlock(newX, newY + deltaY, material, true)
        }
        entity.remove<GravityAffectedTag>() // prevent the block to collide again
        world.engine.removeEntity(entity)
      }
    }
  }
}

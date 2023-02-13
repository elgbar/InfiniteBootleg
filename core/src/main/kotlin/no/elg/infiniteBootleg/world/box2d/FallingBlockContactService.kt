package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import ktx.ashley.remove
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData

object FallingBlockContactService {

  fun handleFallingBlockContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    if (Main.isAuthoritative()) {
      val materialComponent = entity.materialOrNull ?: return
      val block = event.getOtherFixtureUserData<Block>(entity) { true } ?: return
      val newX: Int = block.worldX
      val newY: Int = block.worldY
      var deltaY = 0
      val world = block.world
      synchronized(world) {
        while (!world.isAirBlock(newX, newY + deltaY)) {
          deltaY++
        }
        world.setBlock(newX, newY + deltaY, materialComponent.material, true)
        entity.remove<MaterialComponent>() // prevent the block to collide again
        world.engine.removeEntity(entity)
      }
    }
  }
}

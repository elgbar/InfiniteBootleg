package no.elg.infiniteBootleg.core.world.box2d.service

import com.badlogic.ashley.core.Entity
import ktx.ashley.remove
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent.Companion.spellStateOrNull
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem
import no.elg.infiniteBootleg.protobuf.Packets
import org.jetbrains.annotations.Async

object SpellContactPhysicsEventHandler : PhysicsSystem.PhysicsEventHandler {

  fun handleSpellContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    if (Main.isAuthoritative) {
      val spellState = entity.spellStateOrNull ?: return
      spellState.staff.onSpellLand(spellState, entity, event)
      // remove the spell state component at once to prevent multiple landings
      entity.remove<SpellStateComponent>()
      entity.world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)
    }
  }

  override fun handleEvent(entity: Entity, @Async.Execute event: PhysicsEvent) {
    if (event is PhysicsEvent.ContactBeginsEvent) {
      handleSpellContactBeginsEvent(entity, event)
    }
  }
}

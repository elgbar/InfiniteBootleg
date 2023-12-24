package no.elg.infiniteBootleg.world.box2d.service

import com.badlogic.ashley.core.Entity
import ktx.ashley.remove
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.isType
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.SpellStateComponent.Companion.spellStateOrNull

object SpellContactService {

  fun handleSpellContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    if (Main.isAuthoritative && entity.isType(ProtoWorld.Entity.EntityType.SPELL)) {
      val spellState = entity.spellStateOrNull ?: return
      spellState.staff.spellLand(spellState, entity, event)
      // remove the spell state component at once to prevent multiple landings
      entity.remove<SpellStateComponent>()
      entity.world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)
    }
  }
}

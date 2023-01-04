package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id

/**
 * Despawn an entity if we are currently the server
 */
fun despawnEntity(entity: Entity, despawnReason: DespawnReason) {
  if (Main.isServer()) {
    Main.inst().scheduler.executeAsync {
      broadcast(clientBoundDespawnEntity(entity.id.id, despawnReason))
    }
  }
}

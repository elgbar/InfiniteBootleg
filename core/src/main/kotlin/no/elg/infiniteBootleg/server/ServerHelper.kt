package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly

/**
 * Despawn an entity if we are currently the server
 */
fun despawnEntity(entity: Entity, despawnReason: Packets.DespawnEntity.DespawnReason) {
  if (Main.isServer) {
    val entityId = entity.id
    if (!entity.authoritativeOnly) {
      launchOnAsync {
        broadcast(clientBoundDespawnEntity(entityId, despawnReason))
      }
    }
  }
}

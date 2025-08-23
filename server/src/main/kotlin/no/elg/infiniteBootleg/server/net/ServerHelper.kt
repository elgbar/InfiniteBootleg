package no.elg.infiniteBootleg.server.net

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.net.clientBoundDespawnEntity
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.server.ServerMain

/**
 * Despawn an entity if we are currently the server
 */
fun despawnEntity(entity: Entity, despawnReason: Packets.DespawnEntity.DespawnReason) {
  val entityId = entity.id
  if (!entity.authoritativeOnly) {
    launchOnAsyncSuspendable {
      ServerMain.inst().packetSender.broadcast(clientBoundDespawnEntity(entityId, despawnReason))
    }
  }
}

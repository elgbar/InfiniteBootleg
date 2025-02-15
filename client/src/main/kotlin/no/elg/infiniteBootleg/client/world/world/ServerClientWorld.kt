package no.elg.infiniteBootleg.client.world.world

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import no.elg.infiniteBootleg.client.world.ecs.system.net.SendPlayerVelocities
import no.elg.infiniteBootleg.client.world.loader.chunk.ServerClientChunkLoader
import no.elg.infiniteBootleg.client.world.managers.container.ServerClientWorldContainerManager
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.loader.WorldLoader
import no.elg.infiniteBootleg.core.world.loader.chunk.ChunkLoader
import no.elg.infiniteBootleg.core.world.managers.container.WorldContainerManager
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class ServerClientWorld(protoWorld: ProtoWorld.World, val serverClient: ServerClient) : ClientWorld(protoWorld, forceTransient = true) {

  override val worldContainerManager: WorldContainerManager = ServerClientWorldContainerManager(this)
  override val chunkLoader: ChunkLoader = ServerClientChunkLoader(this, WorldLoader.generatorFromProto(protoWorld))

  override fun additionalSystems(): Set<EntitySystem> = super.additionalSystems() + setOf(SendPlayerVelocities)

  override fun isAuthorizedToChange(entity: Entity): Boolean {
    val entityId = entity.id
    return serverClient.entityId == entityId
  }

  override fun dispose() {
    super.dispose()
    serverClient.dispose()
  }
}

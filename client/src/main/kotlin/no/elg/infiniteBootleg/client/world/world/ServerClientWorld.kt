package no.elg.infiniteBootleg.client.world.world

import com.badlogic.ashley.core.Entity
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

  override fun isAuthorizedToChange(entity: Entity): Boolean {
    val id = entity.id
    return serverClient.entityId == id
  }

  override fun dispose() {
    super.dispose()
    serverClient.dispose()
  }
}

package no.elg.infiniteBootleg.world.world

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.world
import no.elg.infiniteBootleg.server.ServerClient
import no.elg.infiniteBootleg.world.loader.WorldLoader.generatorFromProto
import no.elg.infiniteBootleg.world.loader.chunk.ChunkLoader
import no.elg.infiniteBootleg.world.loader.chunk.ServerClientChunkLoader
import no.elg.infiniteBootleg.world.managers.container.ServerClientWorldContainerManager
import no.elg.infiniteBootleg.world.managers.container.WorldContainerManager

class ServerClientWorld(protoWorld: ProtoWorld.World, val serverClient: ServerClient) : ClientWorld(protoWorld, forceTransient = true) {

  override val worldContainerManager: WorldContainerManager = ServerClientWorldContainerManager(this)
  override val chunkLoader: ChunkLoader = ServerClientChunkLoader(this, generatorFromProto(protoWorld))

  override fun dispose() {
    super.dispose()
    serverClient.dispose()
  }
}

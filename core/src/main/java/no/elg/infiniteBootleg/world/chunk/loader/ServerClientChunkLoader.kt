package no.elg.infiniteBootleg.world.chunk.loader

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.serverBoundChunkRequestPacket
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.compact
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ServerClientWorld
import no.elg.infiniteBootleg.world.generator.ChunkGenerator
import java.util.concurrent.ConcurrentHashMap

class ServerClientChunkLoader(override val world: ServerClientWorld, generator: ChunkGenerator) : ChunkLoader(generator) {

  private val toBeLoadedChunks: ConcurrentHashMap.KeySetView<Long, Boolean> = ConcurrentHashMap.newKeySet()

  override fun fetchChunk(chunkLoc: Long): Chunk? {
    if (chunkLoc in toBeLoadedChunks) {
      return null
    }
    toBeLoadedChunks += chunkLoc
    val chunkX = CoordUtil.decompactLocX(chunkLoc)
    val chunkY = CoordUtil.decompactLocY(chunkLoc)
    val serverClient = world.serverClient
    serverClient.ctx.writeAndFlush(serverClient.serverBoundChunkRequestPacket(chunkX, chunkY))
    return null
  }

  override fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    toBeLoadedChunks -= protoChunk.position.compact()
    return super.loadChunkFromProto(protoChunk)
  }
}

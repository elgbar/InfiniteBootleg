package no.elg.infiniteBootleg.world.loader.chunk

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.serverBoundChunkRequestPacket
import no.elg.infiniteBootleg.util.compact
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.util.concurrent.ConcurrentHashMap

class ServerClientChunkLoader(override val world: ServerClientWorld, generator: ChunkGenerator) : ChunkLoader(generator) {

  private val toBeLoadedChunks: ConcurrentHashMap.KeySetView<Long, Boolean> = ConcurrentHashMap.newKeySet()

  private val loadedChunk = LoadedChunk(null, false)

  override fun fetchChunk(chunkLoc: Long): LoadedChunk {
    if (chunkLoc in toBeLoadedChunks) {
      return loadedChunk
    }
    toBeLoadedChunks += chunkLoc
    val chunkX = chunkLoc.decompactLocX()
    val chunkY = chunkLoc.decompactLocY()
    val serverClient = world.serverClient
    serverClient.ctx.writeAndFlush(serverClient.serverBoundChunkRequestPacket(chunkX, chunkY))
    return loadedChunk
  }

  override fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    toBeLoadedChunks -= protoChunk.position.compact()
    return super.loadChunkFromProto(protoChunk)
  }
}

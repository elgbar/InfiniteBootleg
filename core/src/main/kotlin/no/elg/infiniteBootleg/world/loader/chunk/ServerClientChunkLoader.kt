package no.elg.infiniteBootleg.world.loader.chunk

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.serverBoundChunkRequestPacket
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.time.Duration

class ServerClientChunkLoader(override val world: ServerClientWorld, generator: ChunkGenerator) : ChunkLoader(generator) {

  private val toBeLoadedChunks: Cache<ChunkCompactLoc, Boolean> =
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(1))
      .build()

  private val loadedChunk = LoadedChunk(null, false)

  override fun fetchChunk(chunkLoc: ChunkCompactLoc): LoadedChunk {
    toBeLoadedChunks.getIfPresent(chunkLoc)
    toBeLoadedChunks.get(chunkLoc) {
      val (chunkX, chunkY) = chunkLoc
      val serverClient = world.serverClient
      serverClient.ctx.writeAndFlush(serverClient.serverBoundChunkRequestPacket(chunkX, chunkY))
      true
    }
    return loadedChunk
  }

  override fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    toBeLoadedChunks.invalidate(protoChunk.position.toCompact())
    return super.loadChunkFromProto(protoChunk)
  }
}

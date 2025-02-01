package no.elg.infiniteBootleg.world.loader.chunk

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.elg.infiniteBootleg.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.net.serverBoundChunkRequestPacket
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.time.Duration

class ServerClientChunkLoader(override val world: ServerClientWorld, generator: ChunkGenerator) : ChunkLoader(generator) {

  private val toBeLoadedChunks: Cache<ChunkCompactLoc, LoadedChunk> =
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(1))
      .build()

  override fun fetchChunk(chunkLoc: ChunkCompactLoc): LoadedChunk =
    toBeLoadedChunks.get(chunkLoc) {
      // Abuse cache to only fetch chunk if we haven't done so within the last second
      val (chunkX, chunkY) = chunkLoc
      world.serverClient.sendServerBoundPacket { serverBoundChunkRequestPacket(chunkX, chunkY) }
      NULL_LOADED_CHUNK
    }

  companion object {
    private val NULL_LOADED_CHUNK = LoadedChunk(null, false)
  }
}

package no.elg.infiniteBootleg.world.loader.chunk

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.World

/**
 * Handle saving and loading of chunks.
 *
 *
 * If a chunk is saved to disk then that chunk will be loaded (assuming [Settings.loadWorldFromDisk] is `true`) Otherwise it will be generated with the given [ ]
 *
 * @author Elg
 */
class FullChunkLoader(override val world: World, generator: ChunkGenerator) : ChunkLoader(generator) {

  override fun fetchChunk(chunkLoc: Long): LoadedChunk {
    val (chunkX, chunkY) = chunkLoc
    val loadChunkFromFile = loadChunkFromFile(chunkX, chunkY)
    return LoadedChunk(loadChunkFromFile ?: generateChunk(chunkX, chunkY), loadChunkFromFile == null)
  }

  private fun generateChunk(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? {
    val generated = generator.generate(world, chunkX, chunkY)
    if (generated.isValid) {
      return generated
    }
    Main.logger().warn("Failed to generate chunk ${stringifyCompactLoc(chunkX, chunkY)}")
    return null
  }

  //    @GuardedBy("no.elg.infiniteBootleg.world.world.World.chunksLock.writeLock()")
  fun save(chunk: Chunk) {
    if (Settings.loadWorldFromDisk && chunk.shouldSave()) {
      // only save if valid and changed
      val fh = getChunkFile(world, chunk.chunkX, chunk.chunkY) ?: return
      chunk.save().thenApply {
        fh.writeBytes(it.toByteArray(), false)
      }
    }
  }

  /**
   * @param chunkX The y coordinate of the chunk (in chunk view)
   * @param chunkY The x coordinate of the chunk (in chunk view)
   * @return If a chunk at the given location exists
   */
  private fun existsOnDisk(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
    if (!Settings.loadWorldFromDisk) {
      return false
    }
    val chunkFile = getChunkFile(world, chunkX, chunkY)
    return chunkFile != null && chunkFile.exists() && !chunkFile.isDirectory
  }
}

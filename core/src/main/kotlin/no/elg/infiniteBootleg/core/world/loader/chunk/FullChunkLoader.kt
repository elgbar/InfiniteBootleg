package no.elg.infiniteBootleg.core.world.loader.chunk

import com.google.protobuf.InvalidProtocolBufferException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

/**
 * Handle saving and loading of chunks.
 *
 *
 * If a chunk is saved to disk then that chunk will be loaded (assuming [World.isTransient] is `false`) Otherwise it will be generated
 *
 * @author Elg
 */
class FullChunkLoader(override val world: World, generator: ChunkGenerator) : ChunkLoader(generator) {

  override fun fetchChunk(chunkLoc: ChunkCompactLoc): LoadedChunk {
    val (chunkX, chunkY) = chunkLoc
    val loadChunkFromFile = loadChunkFromFile(chunkX, chunkY)
    return LoadedChunk(loadChunkFromFile ?: generateChunk(chunkX, chunkY), loadChunkFromFile == null)
  }

  private fun loadChunkFromFile(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? {
    if (world.isTransient) {
      return null
    }
    val protoChunk = readChunkFile(chunkX, chunkY)
    return if (protoChunk != null) {
      loadChunkFromProto(protoChunk)
    } else {
      logger.trace { "Chunk ${stringifyCompactLoc(chunkX, chunkY)} did not exist on file" }
      null
    }
  }

  private fun readChunkFile(chunkX: ChunkCoord, chunkY: ChunkCoord): ProtoWorld.Chunk? {
    val chunkFile = getChunkFile(world, chunkX, chunkY)?.file()
    if (chunkFile != null && chunkFile.isFile && chunkFile.canRead()) {
      val bytes = chunkFile.readBytes()
      return try {
        ProtoWorld.Chunk.parseFrom(bytes)
      } catch (e: InvalidProtocolBufferException) {
        logger.error(e) { "Invalid protobuf while reading chunk file" }
        deleteChunkFile(chunkX, chunkY)
        null
      }
    }
    return null
  }

  private fun generateChunk(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? {
    val generated = generator.generate(world, chunkX, chunkY)
    if (generated.isValid) {
      return generated
    }
    logger.warn { "Failed to generate chunk ${stringifyCompactLoc(chunkX, chunkY)}" }
    return null
  }

  //    @GuardedBy("no.elg.infiniteBootleg.core.World.chunksLock.writeLock()")
  override fun save(chunk: Chunk) {
    if (!world.isTransient && chunk.shouldSave()) {
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
    if (world.isTransient) {
      return false
    }
    val chunkFile = getChunkFile(world, chunkX, chunkY)
    return chunkFile != null && chunkFile.exists() && !chunkFile.isDirectory
  }
}

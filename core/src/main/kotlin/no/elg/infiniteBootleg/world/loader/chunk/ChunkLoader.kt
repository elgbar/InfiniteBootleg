package no.elg.infiniteBootleg.world.loader.chunk

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.google.protobuf.InvalidProtocolBufferException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.CorruptChunkException
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.deleteOrLogFile
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.World
import java.io.File

private val logger = KotlinLogging.logger {}

abstract class ChunkLoader(val generator: ChunkGenerator) : Disposable {

  abstract val world: World

  /**
   * Load the chunk at the given chunk location
   *
   * @param chunkLoc The coordinates of the chunk (in chunk view)
   * @return The loaded chunk or null if something went wrong
   */
  abstract fun fetchChunk(chunkLoc: ChunkCompactLoc): LoadedChunk

  fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    val chunkPosition = protoChunk.position
    val chunk = ChunkImpl(world, chunkPosition.x, chunkPosition.y)

    try {
      if (fullyLoadChunk(chunk, protoChunk)) {
        return chunk
      }
    } catch (e: CorruptChunkException) {
      logger.error(e) { "Found a corrupt chunk ${stringifyCompactLoc(chunkPosition)}" }
      deleteChunkFile(chunkPosition.x, chunkPosition.y)
      return null
    }
    logger.warn { "Failed to load chunk ${stringifyCompactLoc(chunkPosition)} from a proto chunk" }
    return null
  }

  protected fun loadChunkFromFile(chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk? {
    val protoChunk = readChunkFile(chunkX, chunkY)
    return if (protoChunk != null) {
      loadChunkFromProto(protoChunk)
    } else {
      logger.trace { "Chunk ${stringifyCompactLoc(chunkX, chunkY)} did not exist on file" }
      null
    }
  }

  private fun fullyLoadChunk(chunk: ChunkImpl, protoChunk: ProtoWorld.Chunk): Boolean {
    if (chunk.load(protoChunk)) {
      chunk.finishLoading()
      return chunk.isValid
    }
    return false
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

  private fun deleteChunkFile(chunkX: ChunkCoord, chunkY: ChunkCoord) {
    getChunkFile(world, chunkX, chunkY)?.let(::deleteOrLogFile)
  }

  override fun dispose() {
    if (generator is Disposable) {
      generator.dispose()
    }
  }

  companion object {

    fun getChunkFile(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): FileHandle? {
      val worldFile = world.worldFolder ?: return null
      return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY)
    }

    private const val CHUNK_FOLDER = "chunks"
  }
}

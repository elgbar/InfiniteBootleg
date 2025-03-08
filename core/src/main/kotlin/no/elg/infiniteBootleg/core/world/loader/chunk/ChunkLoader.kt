package no.elg.infiniteBootleg.core.world.loader.chunk

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.exceptions.CorruptChunkException
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.deleteOrLogFile
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
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

  /**
   * Save the given chunk to the disk
   */
  open fun save(chunk: Chunk) = Unit

  fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    val chunkPosition = protoChunk.position
    val chunk = Main.Companion.inst().chunkFactory.createChunk(world, chunkPosition.x, chunkPosition.y)

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

  private fun fullyLoadChunk(chunk: ChunkImpl, protoChunk: ProtoWorld.Chunk): Boolean {
    if (chunk.load(protoChunk)) {
      chunk.finishLoading()
      return chunk.isValid
    }
    return false
  }

  protected fun deleteChunkFile(chunkX: ChunkCoord, chunkY: ChunkCoord) {
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

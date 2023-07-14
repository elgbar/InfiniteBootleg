package no.elg.infiniteBootleg.world.loader.chunk

import com.badlogic.gdx.files.FileHandle
import com.google.protobuf.InvalidProtocolBufferException
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.world.World
import java.io.File

abstract class ChunkLoader(val generator: ChunkGenerator) {

  abstract val world: World

  /**
   * Load the chunk at the given chunk location
   *
   * @param chunkLoc The coordinates of the chunk (in chunk view)
   * @return The loaded chunk or null if something went wrong
   */
  abstract fun fetchChunk(chunkLoc: Long): LoadedChunk

  open fun loadChunkFromProto(protoChunk: ProtoWorld.Chunk): Chunk? {
    val chunkPosition = protoChunk.position
    val chunk = ChunkImpl(world, chunkPosition.x, chunkPosition.y)
    if (fullyLoadChunk(chunk, protoChunk)) {
      return chunk
    }
    Main.logger().warn("Failed to load chunk ${stringifyCompactLoc(chunkPosition)} from a proto chunk")
    return null
  }

  protected fun loadChunkFromFile(chunkX: Int, chunkY: Int): Chunk? {
    val protoChunk = readChunkFile(chunkX, chunkY)
    return if (protoChunk != null) {
      loadChunkFromProto(protoChunk)
    } else {
//      Main.logger().trace("ChunkLoader") { "Chunk ${stringifyCompactLoc(chunkX, chunkY)} did not exist on file" }
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

  private fun readChunkFile(chunkX: Int, chunkY: Int): ProtoWorld.Chunk? {
    val chunkFile = getChunkFile(world, chunkX, chunkY)?.file()
    if (chunkFile != null && chunkFile.isFile && chunkFile.canRead()) {
      val bytes = chunkFile.readBytes()
      return try {
        ProtoWorld.Chunk.parseFrom(bytes)
      } catch (e: InvalidProtocolBufferException) {
        e.printStackTrace()
        null
      }
    }
    return null
  }

  companion object {
    @JvmStatic
    fun getChunkFile(world: World, chunkX: Int, chunkY: Int): FileHandle? {
      val worldFile = world.worldFolder ?: return null
      return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY)
    }

    private const val CHUNK_FOLDER = "chunks"
  }
}

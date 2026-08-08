package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.exceptions.CorruptChunkColumnException
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

class ChunkColumnsManager(val world: World) : Disposable {

  /**
   * Must be accessed under `synchronized(chunkColumns)`
   */
  private val chunkColumns = IntMap<ChunkColumn>()
  private val chunkColumnListeners = ChunkColumnListeners()

  fun fromProtobuf(columns: List<ProtoWorld.ChunkColumn>) {
    synchronized(chunkColumns) {
      var foundChunkXColumn = false
      for (protoCC in columns) {
        try {
          if (protoCC.chunkX == 0) {
            if (foundChunkXColumn) {
              throw CorruptChunkColumnException("Got multiple chunks columns with chunkX=0. Discarding all but the first zero chunk column")
            }
            foundChunkXColumn = true
          }
          val chunkColumn = ChunkColumnImpl.fromProtobuf(world, protoCC)
          chunkColumns.put(chunkColumn.chunkX, chunkColumn)
        } catch (e: CorruptChunkColumnException) {
          logger.warn(e) { "Corrupt chunk column loaded at chunk x ${protoCC.chunkX}, ignoring it" }
        }
      }
    }
  }

  fun toProtobuf(): List<ProtoWorld.ChunkColumn> {
    synchronized(chunkColumns) {
      return chunkColumns.values().map { it.toProtobuf() }
    }
  }

  fun getChunkColumn(chunkX: ChunkCoord): ChunkColumn {
    try {
      // Fast path, though this is not thread safe so we must validate the result
      val fastColumn = chunkColumns.get(chunkX)
      if (fastColumn != null && fastColumn.chunkX == chunkX) {
        return fastColumn
      }
    } catch (_: Exception) {
      // ignore, we try safe path if anything happens
    }
    synchronized(chunkColumns) {
      // The column might have been updated while we've been waiting for the lock
      val column = chunkColumns[chunkX]
      if (column == null) {
        val newCol: ChunkColumn = ChunkColumnImpl(world, chunkX, null, null)
        chunkColumns.put(chunkX, newCol)
        return newCol
      }
      return column
    }
  }

  override fun dispose() {
    chunkColumnListeners.dispose()
  }
}

package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.world.chunks.ChunkColumnImpl.Companion.fromProtobuf
import no.elg.infiniteBootleg.world.world.World

class ChunkColumnsManager(val world: World) : Disposable {

  /**
   * Must be accessed under `synchronized(chunkColumns)`
   */
  private val chunkColumns = IntMap<ChunkColumn>()
  private val chunkColumnListeners = ChunkColumnListeners()

  fun fromProtobuf(columns: List<ProtoWorld.ChunkColumn>) {
    synchronized(chunkColumns) {
      for (protoCC in columns) {
        val chunkColumn = fromProtobuf(world, protoCC)
        val chunkX = protoCC.chunkX
        chunkColumns.put(chunkX, chunkColumn)
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
    } catch (ignore: Exception) {
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
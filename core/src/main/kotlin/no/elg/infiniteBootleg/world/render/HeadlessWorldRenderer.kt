package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.iterator
import no.elg.infiniteBootleg.world.world.ServerWorld
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Elg
 */
class HeadlessWorldRenderer(override val world: ServerWorld) : WorldRender {
  private val viewingChunks: ObjectMap<String, ServerClientChunksInView> = OrderedMap()
  private val lock = ReentrantReadWriteLock()
  private val readLock: Lock = lock.readLock()
  private val writeLock: Lock = lock.writeLock()

  @Synchronized
  override fun render() {
    // Note to self: do not call chunkBody#update while under the chunksLock.readLock() or
    // chunksLock.writeLock()
    for (chunk in world.loadedChunks) {
      if (chunk.isValid && chunk.isDirty) {
        chunk.chunkBody.update()
      }
    }
  }

  override fun dispose() {}
  override fun resize(width: Int, height: Int) {}
  override fun update() {
    readLock.lock()
    try {
      for (entity in world.engine.getEntitiesFor(playerFamily)) {
        val chunksInView = viewingChunks.get(entity.id)
        if (chunksInView != null) {
          val (x, y) = entity.positionComponent
          chunksInView.setCenter(x.worldToChunk(), y.worldToChunk())
        }
      }
    } finally {
      readLock.unlock()
    }
  }

  override fun isOutOfView(chunk: Chunk): Boolean = isOutOfView(chunk.chunkX, chunk.chunkY)

  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
    readLock.lock()
    return try {
      for (inView in viewingChunks.values()) {
        if (inView.isInView(chunkX, chunkY)) {
          return false
        }
      }
      true
    } finally {
      readLock.unlock()
    }
  }

  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = !isOutOfView(chunkX, chunkY)

  override val chunkLocationsInView: Iterator<Long>
    get() = viewingChunks.values().flatMap { it.iterator().asSequence() }.iterator()

  fun addClient(uuid: String, civ: ServerClientChunksInView) {
    writeLock.lock()
    try {
      viewingChunks.put(uuid, civ)
    } finally {
      writeLock.unlock()
    }
  }

  fun removeClient(uuid: String) {
    writeLock.lock()
    try {
      viewingChunks.remove(uuid)
    } finally {
      writeLock.unlock()
    }
  }

  fun getClient(uuid: String): ServerClientChunksInView? {
    readLock.lock()
    return try {
      viewingChunks.get(uuid)
    } finally {
      readLock.unlock()
    }
  }
}

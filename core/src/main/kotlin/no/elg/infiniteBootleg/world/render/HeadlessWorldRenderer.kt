package no.elg.infiniteBootleg.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.chunkColumnsInView
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.iterator
import no.elg.infiniteBootleg.world.world.ServerWorld
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author Elg
 */
class HeadlessWorldRenderer(override val world: ServerWorld) : WorldRender {
  private val viewingChunks: ObjectMap<String, ServerClientChunksInView> = OrderedMap()
  private val lock = ReentrantReadWriteLock()

  private val entities: ImmutableArray<Entity> = world.engine.getEntitiesFor(playerFamily)

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
    lock.read {
      for (entity in entities) {
        val chunksInView = viewingChunks.get(entity.id)
        if (chunksInView != null) {
          val (x, y) = entity.positionComponent
          chunksInView.setCenter(x.worldToChunk(), y.worldToChunk())
        }
      }
    }
  }

  override fun isOutOfView(chunk: Chunk): Boolean = isOutOfView(chunk.chunkX, chunk.chunkY)

  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
    return lock.read {
      for (inView in viewingChunks.values()) {
        if (inView.isInView(chunkX, chunkY)) {
          return false
        }
      }
      true
    }
  }

  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = !isOutOfView(chunkX, chunkY)

  override val chunkLocationsInView: Iterator<Long>
    get() = viewingChunks.values().flatMap { it.iterator().asSequence() }.iterator()

  override val chunkColumnsInView: Set<ChunkCoord>
    get() = viewingChunks.values().flatMapTo(mutableSetOf()) { it.chunkColumnsInView() }

  fun addClient(uuid: String, civ: ServerClientChunksInView) {
    lock.write {
      viewingChunks.put(uuid, civ)
    }
  }

  fun removeClient(uuid: String) {
    lock.write {
      viewingChunks.remove(uuid)
    }
  }

  fun getClient(uuid: String): ServerClientChunksInView? {
    return lock.read {
      viewingChunks.get(uuid)
    }
  }
}

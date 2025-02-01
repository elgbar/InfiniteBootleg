package no.elg.infiniteBootleg.server.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.WorldSpawnUpdatedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.util.worldToChunkX
import no.elg.infiniteBootleg.core.util.worldToChunkY
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.playerFamily
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.chunkColumnsInView
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.iterator
import no.elg.infiniteBootleg.core.world.render.ServerClientChunksInView
import no.elg.infiniteBootleg.core.world.render.WorldRender
import no.elg.infiniteBootleg.server.world.ServerWorld
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class HeadlessWorldRenderer(override val world: ServerWorld) : WorldRender {
  private val viewingChunks: ObjectMap<String, ServerClientChunksInView> = OrderedMap()

  private val lock = ReentrantReadWriteLock()

  private val entities: ImmutableArray<Entity> = world.engine.getEntitiesFor(playerFamily)

  private val spawnChunksInView = world.spawn.worldToChunk().let { (chunkX, chunkY) ->
    ServerClientChunksInView(chunkX, chunkY)
  }

  private val onSpawnChanged = EventManager.registerListener { event: WorldSpawnUpdatedEvent ->
    val newChunkX = event.newSpawn.worldToChunkX()
    val newChunkY = event.newSpawn.worldToChunkY()
    if (spawnChunksInView.centerX != newChunkX || spawnChunksInView.centerY != newChunkY) {
      spawnChunksInView.setCenter(newChunkX, newChunkY)
      logger.debug {
        "Updating spawn chunks in view, new center and world chunk spawn at ${
          stringifyCompactLoc(
            newChunkX,
            newChunkY
          )
        }"
      }
    } else {
      logger.debug { "World spawn updated, but its in the same chunk as before. Will not update spawn chunks in view" }
    }
  }

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

  override fun dispose() {
    onSpawnChanged.removeListener()
  }

  override fun resize(width: Int, height: Int) = Unit
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
      for (inView in allChunksInView()) {
        if (inView.isInView(chunkX, chunkY)) {
          return false
        }
      }
      true
    }
  }

  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = !isOutOfView(chunkX, chunkY)

  private fun allChunksInView(): Sequence<ServerClientChunksInView> =
    sequenceOf(
      sequenceOf(spawnChunksInView),
      Sequence { viewingChunks.values().iterator() }
    ).flatten()

  override val chunkLocationsInView: Iterator<Long>
    get() = allChunksInView().flatMap { it.iterator().asSequence() }.iterator()

  override val chunkColumnsInView: Set<ChunkCoord>
    get() = allChunksInView().flatMapTo(mutableSetOf()) { it.chunkColumnsInView() }

  fun addClient(entityId: String, civ: ServerClientChunksInView) {
    lock.write {
      viewingChunks.put(entityId, civ)
    }
  }

  fun removeClient(entityId: String) {
    lock.write {
      viewingChunks.remove(entityId)
    }
  }

  fun getClient(entityId: String): ServerClientChunksInView? {
    return lock.read {
      viewingChunks.get(entityId)
    }
  }
}

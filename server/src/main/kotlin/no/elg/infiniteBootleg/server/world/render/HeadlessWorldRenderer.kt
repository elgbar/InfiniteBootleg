package no.elg.infiniteBootleg.server.world.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
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
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.chunkColumnsInView
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.sequence
import no.elg.infiniteBootleg.core.world.render.ServerClientChunksInView
import no.elg.infiniteBootleg.core.world.render.WorldRender
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.server.world.ecs.components.transients.ServerClientChunksInViewComponent.Companion.chunksInView
import no.elg.infiniteBootleg.server.world.ecs.system.InViewFamily

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class HeadlessWorldRenderer(override val world: ServerWorld) : WorldRender {

  private val inViewEntities: ImmutableArray<Entity> = world.engine.getEntitiesFor(InViewFamily)

  private val spawnChunksInView: ServerClientChunksInView = world.spawn.worldToChunk().let { (chunkX, chunkY) ->
    ServerClientChunksInView(chunkX, chunkY)
  }

  private val onSpawnChanged = EventManager.registerListener { event: WorldSpawnUpdatedEvent ->
    val newChunkX = event.newSpawn.worldToChunkX()
    val newChunkY = event.newSpawn.worldToChunkY()
    if (spawnChunksInView.centerX != newChunkX || spawnChunksInView.centerY != newChunkY) {
      spawnChunksInView.setCenter(newChunkX, newChunkY)
      logger.debug {
        "Updating spawn chunks in view, new center and world chunk spawn at ${stringifyCompactLoc(newChunkX, newChunkY)}"
      }
    } else {
      logger.debug { "World spawn updated, but its in the same chunk as before. Will not update spawn chunks in view" }
    }
  }

  override fun render() {
    // Note to self: do not call chunkBody#update while under the chunksLock.readLock() or chunksLock.writeLock()
    val filter = world.loadedChunks.filter { chunk -> chunk.isValid && chunk.isDirty }
    if (filter.isNotEmpty()) {
      logger.debug { "Updating ${filter.size} dirty chunks" }
      for (chunk in filter) {
        chunk.updateIfDirty()
        chunk.chunkBody.update()
      }
    }
  }

  override fun dispose() {
    onSpawnChanged.removeListener()
  }

  override fun resize(width: Int, height: Int) = Unit
  override fun update() = Unit

  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean =
    spawnChunksInView.isOutOfView(chunkX, chunkY) && world.engine.doUnderEngineLock {
      inViewEntities.all { it.chunksInView.isOutOfView(chunkX, chunkY) }
    }

  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean =
    spawnChunksInView.isInView(chunkX, chunkY) || world.engine.doUnderEngineLock {
      inViewEntities.any { it.chunksInView.isInView(chunkX, chunkY) }
    }

  private fun allChunksInView(): Sequence<ServerClientChunksInView> =
    sequenceOf(spawnChunksInView) + world.engine.doUnderEngineLock { inViewEntities.map { it.chunksInView }.asSequence() }

  override val chunkLocationsInView: Sequence<Long>
    get() = allChunksInView().flatMap { it.sequence() }.distinct()

  override val chunkColumnsInView: Set<ChunkCoord>
    get() = allChunksInView().flatMapTo(mutableSetOf()) { it.chunkColumnsInView() }
}

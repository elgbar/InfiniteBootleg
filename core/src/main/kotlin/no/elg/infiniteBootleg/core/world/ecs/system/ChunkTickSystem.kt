package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.EntitySystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ViewableChunk
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.world.World

private val logger = KotlinLogging.logger {}

class ChunkTickSystem(private val world: World) : EntitySystem(UPDATE_PRIORITY_LAST) {

  override fun update(deltaTime: Float) {
    val tps = world.worldTicker.tps
    val chunkUnloadTime = tps * CHUNK_UNLOAD_SECONDS
    val tick = world.worldTicker.tickId
    var unloadQuota = ALLOWED_NORMAL_CHUNK_UNLOADS_PER_SECOND / tps

    fun shouldUnloadChunk(chunk: Chunk): Boolean =
      !chunk.isDisposed && chunk.allowedToUnload && world.render.isOutOfView(chunk) && (chunk is ViewableChunk && tick - chunk.lastViewedTick > chunkUnloadTime)

    world.readChunks {
      val iterator = it.values
      for (chunk: Chunk? in iterator) {
        if (chunk == null) {
          logger.warn { "Found null chunk when ticking world" }
          iterator.remove(null)
        } else if (chunk.isDisposed) {
          logger.warn { "Found disposed chunk ${stringifyCompactLoc(chunk)} when ticking world" }
          launchOnAsyncSuspendable {
            world.unloadChunk(chunk, force = true)
          }
          unloadQuota--
        } else {
          chunk.flushPendingLightUpdates()
          if (unloadQuota > 0 && shouldUnloadChunk(chunk)) {
            unloadQuota--
            launchOnAsyncSuspendable {
              if (shouldUnloadChunk(chunk)) {
                world.unloadChunk(chunk)
              }
            }
          }
        }
      }
    }
  }

  companion object {
    private const val CHUNK_UNLOAD_SECONDS = 30L
    private const val ALLOWED_NORMAL_CHUNK_UNLOADS_PER_SECOND = 500
  }
}

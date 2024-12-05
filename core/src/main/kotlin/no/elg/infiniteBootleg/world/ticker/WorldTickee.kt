package no.elg.infiniteBootleg.world.ticker

import com.badlogic.gdx.utils.LongMap
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.events.WorldTickedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.world.World

private val logger = KotlinLogging.logger {}

internal class WorldTickee(private val world: World) : Ticking {

  @GuardedBy("world.chunksLock")
  private val chunkIterator: LongMap.Entries<Chunk> = world.createChunkIterator()
  private val worldTickedEvent = WorldTickedEvent(world)

  @Synchronized
  override fun tick() {
    val chunkUnloadTime = world.worldTicker.tps * CHUNK_UNLOAD_SECONDS

    // tick all chunks and blocks in chunks
    val tick = world.worldTicker.tickId

    world.readChunks {
      chunkIterator.reset()
      while (chunkIterator.hasNext()) {
        val chunk: Chunk? = chunkIterator.next().value

        // clean up dead chunks
        if (chunk == null) {
          logger.warn { "Found null chunk when ticking world" }
          continue
        } else if (chunk.isDisposed) {
          logger.warn { "Found disposed chunk ${stringifyCompactLoc(chunk)} when ticking world" }
          launchOnAsync {
            world.unloadChunk(chunk, force = true)
          }
          continue
        }
        if (chunk.isAllowedToUnload && world.render.isOutOfView(chunk) && tick - chunk.lastViewedTick > chunkUnloadTime) {
          launchOnAsync {
            world.unloadChunk(chunk)
          }
          continue
        }
      }
    }
    dispatchEvent(worldTickedEvent)
  }

  override fun tickRare() {
    val time = world.worldTime
    time.time += world.worldTicker.secondsDelayBetweenTicks * time.timeScale
  }

  companion object {
    private const val CHUNK_UNLOAD_SECONDS = 30L
  }
}

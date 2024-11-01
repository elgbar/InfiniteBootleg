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
import kotlin.concurrent.read

private val logger = KotlinLogging.logger {}

internal class WorldTickee(private val world: World) : Ticking {

  @GuardedBy("world.chunksLock")
  private val chunkIterator: LongMap.Entries<Chunk> = LongMap.Entries(world.chunks)
  private val worldTickedEvent = WorldTickedEvent(world)

  @Synchronized
  override fun tick() {
    val chunkUnloadTime = world.worldTicker.tps * 5

    // tick all chunks and blocks in chunks
    val tick = world.worldTicker.tickId

    world.chunksLock.read {
      chunkIterator.reset()
      while (chunkIterator.hasNext()) {
        val chunk: Chunk? = chunkIterator.next().value

        // clean up dead chunks
        if (chunk == null) {
          logger.warn { "Found null chunk when ticking world" }
          chunkIterator.remove()
          continue
        } else if (chunk.isDisposed) {
          logger.warn { "Found disposed chunk ${stringifyCompactLoc(chunk.chunkX, chunk.chunkY)} when ticking world" }
          chunkIterator.remove()
          continue
        }
        if (chunk.isAllowedToUnload && world.render.isOutOfView(chunk) && tick - chunk.lastViewedTick > chunkUnloadTime) {
          chunkIterator.remove()
          launchOnAsync {
            world.unloadChunk(chunk, force = false, save = true)
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
}

package no.elg.infiniteBootleg.world.ticker

import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.world.World
import javax.annotation.concurrent.GuardedBy

internal class WorldTickee(private val world: World) : Ticking {

  @GuardedBy("world.chunksLock")
  private val chunkIterator: LongMap.Entries<Chunk> = LongMap.Entries(world.chunks)

  @Synchronized
  override fun tick() {
    val wr = world.render
    val chunkUnloadTime = world.worldTicker.tps * 5

    // tick all chunks and blocks in chunks
    val tick = world.worldTicker.tickId
    world.chunksLock.writeLock().lock()
    try {
      chunkIterator.reset()
      while (chunkIterator.hasNext()) {
        val chunk = chunkIterator.next().value

        // clean up dead chunks
        if (chunk == null) {
          Main.logger().warn("Found null chunk when ticking world")
          chunkIterator.remove()
          continue
        } else if (chunk.isDisposed) {
          Main.logger()
            .warn(
              "Found disposed chunk (" +
                chunk.chunkX +
                "," +
                chunk.chunkY +
                ") when ticking world"
            )
          chunkIterator.remove()
          continue
        }
        if (chunk.isAllowedToUnload &&
          wr.isOutOfView(chunk) && tick - chunk.lastViewedTick > chunkUnloadTime
        ) {
          chunkIterator.remove()
          world.unloadChunk(chunk, false, true)
          continue
        }
        chunk.tick()
      }
    } finally {
      world.chunksLock.writeLock().unlock()
    }
  }

  override fun tickRare() {
    //      if(Main.isServer()){
    //        Main.logger().debug("PACKET INFO", "Server received " +
    // ServerBoundHandler.packetsReceived+" packets");
    //        ServerBoundHandler.packetsReceived = 0;
    //      }
    for (chunk in world.loadedChunks) {
      chunk.tickRare()
    }
    val time = world.worldTime
    time.time = (
      time.time +
        world.worldTicker.secondsDelayBetweenTicks * time.timeScale
      )
  }
}

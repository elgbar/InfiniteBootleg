package no.elg.infiniteBootleg.world

import ktx.collections.GdxArray
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import javax.annotation.concurrent.GuardedBy

@Suppress("GDXKotlinUnsafeIterator")
class TickingBlocks {

  @GuardedBy("itself")
  private val tickingBlocks = GdxArray<TickingBlock>(false, Chunk.CHUNK_SIZE)

  fun clear() {
    synchronized(tickingBlocks) { tickingBlocks.clear() }
  }

  fun setAll(blocks: Array<Array<Block?>>) {
    synchronized(tickingBlocks) {
      tickingBlocks.clear()
      tickingBlocks.ensureCapacity(Chunk.CHUNK_SIZE)
      for (x in 0 until Chunk.CHUNK_SIZE) {
        for (y in 0 until Chunk.CHUNK_SIZE) {
          val block = blocks[x][y]
          if (block is TickingBlock) {
            tickingBlocks.add(block)
          }
        }
      }
    }
  }

  fun setAsync(block: TickingBlock) {
    Main.inst().scheduler.executeAsync {
      synchronized(tickingBlocks) {
        tickingBlocks.add(block)
      }
    }
  }

  fun removeAsync(block: TickingBlock) {
    Main.inst().scheduler.executeAsync {
      synchronized(tickingBlocks) {
        tickingBlocks.removeValue(block, true)
      }
    }
  }

  fun tick(rare: Boolean) {
    synchronized(tickingBlocks) {
      for (block in tickingBlocks) {
        block.tryTick(rare)
      }
    }
  }
}

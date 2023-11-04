package no.elg.infiniteBootleg.world.chunks

import no.elg.infiniteBootleg.events.BlockChangedEvent
import no.elg.infiniteBootleg.events.api.EventListener
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatingEvent
import no.elg.infiniteBootleg.util.directionTo
import no.elg.infiniteBootleg.util.findWhichInnerEdgesOfChunk
import no.elg.infiniteBootleg.util.isNeighbor
import no.elg.infiniteBootleg.util.isNextTo
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.HorizontalDirection
import no.elg.infiniteBootleg.world.VerticalDirection
import no.elg.infiniteBootleg.world.world.World

val ChunkImpl.updateChunkLightEventListener: EventListener<ChunkLightUpdatingEvent>
  get() = EventListener { (chunk, originLocalX, originLocalY): ChunkLightUpdatingEvent ->
    if (this.isNeighbor(chunk)) {
      val dirToThis = chunk.directionTo(this)
      val localX = originLocalX + dirToThis.dx * World.LIGHT_SOURCE_LOOK_BLOCKS
      val localY = originLocalY + dirToThis.dy * World.LIGHT_SOURCE_LOOK_BLOCKS
      val withinHorizontally = when (dirToThis.horizontalDirection) {
        HorizontalDirection.WESTWARD -> localX <= 0
        HorizontalDirection.HORIZONTALLY_ALIGNED -> true
        HorizontalDirection.EASTWARD -> localX >= Chunk.CHUNK_SIZE
      }
      val withinVertically = when (dirToThis.verticalDirection) {
        VerticalDirection.NORTHWARD -> localY >= Chunk.CHUNK_SIZE
        VerticalDirection.VERTICALLY_ALIGNED -> true
        VerticalDirection.SOUTHWARD -> localY <= 0
      }
      if (withinHorizontally && withinVertically) {
        doUpdateLight(chunk.getWorldX(originLocalX), chunk.getWorldY(originLocalY), checkDistance = true, dispatchEvent = false)
      }
    }
  }

val ChunkImpl.blockChangedEventListener: EventListener<BlockChangedEvent>
  get() = EventListener { (oldBlock, newBlock): BlockChangedEvent ->
    val block = oldBlock ?: newBlock ?: return@EventListener

    if (block.isNextTo(this)) {
      val changeDirection = block.findWhichInnerEdgesOfChunk()
      if (Direction.direction(block.chunk.chunkX, block.chunk.chunkY, chunkX, chunkY) in changeDirection) {
        dirty()
      }
    }
  }

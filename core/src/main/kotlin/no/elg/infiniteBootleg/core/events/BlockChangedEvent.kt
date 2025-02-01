package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.AsyncEvent
import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.world.blocks.Block

/**
 * Indicates that a block in a chunk have been modified.
 *
 * This event will not be triggered when a non-existing block becomes an air block or the other way around
 */
data class BlockChangedEvent(val oldBlock: Block?, val newBlock: Block?) : ReasonedEvent, AsyncEvent(ThreadType.TICKER, ThreadType.ASYNC, ThreadType.PHYSICS) {

  val oldOrNewBlock get() = oldBlock ?: newBlock
  val chunk get() = oldOrNewBlock?.chunk
  val world get() = oldOrNewBlock?.world

  override val reason: String
    get() = "Block changed from ${oldBlock?.material} to ${newBlock?.material}"
}

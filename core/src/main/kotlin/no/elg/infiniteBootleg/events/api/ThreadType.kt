package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.ticker.WorldBox2DTicker.Companion.BOX2D_TICKER_TAG_PREFIX
import no.elg.infiniteBootleg.world.ticker.WorldTicker.WORLD_TICKER_TAG_PREFIX

/**
 * What kind of thread an event originated from.
 */
enum class ThreadType {
  /**
   * The event was distracted on the render thread
   */
  RENDER,

  /**
   * The event was distracted from the Box2D physics thread
   */
  PHYSICS,

  /**
   * The event was dispatched from a world thread, exactly which world is unknown. These kind of events are triggered by the world ticking
   */
  TICKER,

  /**
   * The event was distracted from a short-lived pool thread, this is the only event which can be considered truly async
   */
  ASYNC,

  /**
   * Failed to detect what kind of thread the event was dispatched from
   */
  UNKNOWN;

  companion object {
    fun currentThreadType(): ThreadType {
      val threadName = Thread.currentThread().name
      if (threadName == Main.inst().renderThreadName) {
        return RENDER
      } else if (threadName.startsWith(BOX2D_TICKER_TAG_PREFIX, false)) {
        return PHYSICS
      } else if (threadName.startsWith(WORLD_TICKER_TAG_PREFIX, false)) {
        return TICKER
      } else if ("pool" in threadName.lowercase()) {
        return ASYNC
      }
      Main.logger().warn("Dispatched event from unknown thread: $threadName")
      return UNKNOWN
    }
  }
}

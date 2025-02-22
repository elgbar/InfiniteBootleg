package no.elg.infiniteBootleg.core.events.api

import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.async.MainDispatcher
import no.elg.infiniteBootleg.core.exceptions.CalledFromWrongThreadTypeException
import no.elg.infiniteBootleg.core.util.ASYNC_THREAD_NAME
import no.elg.infiniteBootleg.core.util.EVENTS_THREAD_NAME
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker

private val logger = KotlinLogging.logger {}

/**
 * What kind of thread an event originated from.
 */
enum class ThreadType {
  /**
   * The event was distracted on the render thread
   */
  RENDER,

  /**
   * The event was distracted from the Box2D physics thread and ashley engine
   */
  PHYSICS,

  /**
   * The event was dispatched from a world thread, exactly which world is unknown. These kind of events are triggered by the world ticking
   */
  TICKER,

  /**
   * The event was distracted from the async thread, this is the only event which can be considered truly async
   */
  ASYNC,

  /**
   * Failed to detect what kind of thread the event was dispatched from
   */
  UNKNOWN;

  companion object {

    fun isCurrentThreadType(expected: ThreadType): Boolean = currentThreadType() == expected

    fun requireCorrectThreadType(
      expected: ThreadType,
      message: () -> String = { "Expected event to be dispatched from $expected but was dispatched from ${currentThreadType()}" }
    ) {
      if (!isCurrentThreadType(expected)) {
        throw CalledFromWrongThreadTypeException(message())
      }
    }

    private fun isThreadNameAsync(threadName: String): Boolean =
      ASYNC_THREAD_NAME == threadName ||
        EVENTS_THREAD_NAME == threadName ||
        threadName.startsWith("DefaultDispatcher", false) ||
        threadName.startsWith(ASYNC_THREAD_NAME, false) ||
        threadName.startsWith(EVENTS_THREAD_NAME, false)

    fun currentThreadType(): ThreadType {
      val currentThread = Thread.currentThread()
      val threadName = currentThread.name
      if (currentThread === MainDispatcher.mainThread) {
        return RENDER
      } else if (threadName.startsWith(WorldBox2DTicker.Companion.BOX2D_TICKER_TAG_PREFIX, false)) {
        return PHYSICS
      } else if (threadName.startsWith(WorldTicker.Companion.WORLD_TICKER_TAG_PREFIX, false)) {
        return TICKER
      } else if (isThreadNameAsync(threadName)) {
        return ASYNC
      }
      logger.error { "Dispatched event from unknown thread: $threadName" }
      return UNKNOWN
    }
  }
}

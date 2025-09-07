package no.elg.infiniteBootleg.core.events.api

import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.async.MainDispatcher
import ktx.log.error
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.exceptions.CalledFromWrongThreadTypeException
import no.elg.infiniteBootleg.core.util.ASYNC_THREAD_NAME
import no.elg.infiniteBootleg.core.util.EVENTS_THREAD_NAME
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnBox2d
import no.elg.infiniteBootleg.core.util.launchOnBox2dSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.launchOnWorldTicker
import no.elg.infiniteBootleg.core.util.launchOnWorldTickerSuspendable
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

  /**
   * Run the given [block] on this thread type, if already on the correct thread type the block will be run immediately
   */
  fun launchOrRun(block: () -> Unit) = launchOnThreadType(this, block)

  /**
   * Run the given [block] on this thread type, if already on the correct thread type the block will be run immediately
   */
  suspend fun launchSuspendedOrRun(block: suspend () -> Unit) = launchOnThreadType(this, block)

  companion object {

    suspend fun launchOnThreadType(expected: ThreadType, block: suspend () -> Unit) {
      if (isCurrentThreadType(expected)) {
        block()
      } else {
        when (expected) {
          ASYNC -> launchOnAsyncSuspendable(block = { block() })
          PHYSICS -> launchOnBox2dSuspendable(block = { block() })
          RENDER -> launchOnMainSuspendable(block = { block() })
          TICKER -> launchOnWorldTickerSuspendable(block = { block() })
          UNKNOWN -> error { "Don't know how to do a task on $expected thread type" }
        }
      }
    }

    fun launchOnThreadType(expected: ThreadType, block: () -> Unit) {
//      contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
      if (isCurrentThreadType(expected)) {
        block()
      } else {
        when (expected) {
          ASYNC -> launchOnAsyncSuspendable { block() }
          PHYSICS -> launchOnBox2d(block)
          RENDER -> launchOnMain(block)
          TICKER -> launchOnWorldTicker(block)
          UNKNOWN -> error { "Don't know how to do a task on $expected thread type" }
        }
      }
    }

    fun isCurrentThreadType(expected: ThreadType): Boolean = currentThreadType() == expected

    /**
     * Makes sure this code is called from the [expected] thread type.
     *
     * May be disabled in [Settings.assertThreadType]
     *
     * @throws CalledFromWrongThreadTypeException If this is called from another thread type than the [expected]
     */
    fun requireCorrectThreadType(expected: ThreadType, message: (() -> String)? = null) {
      if (Settings.assertThreadType) {
        val current = currentThreadType()
        if (current != expected) {
          val string = "Expected event to be dispatched from $expected but was dispatched from $current"
          val message = message?.invoke() ?: ""
          throw CalledFromWrongThreadTypeException("$message $string")
        }
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
      } else if (threadName.startsWith(WorldBox2DTicker.BOX2D_TICKER_TAG_PREFIX, false)) {
        return PHYSICS
      } else if (threadName.startsWith(WorldTicker.WORLD_TICKER_TAG_PREFIX, false)) {
        return TICKER
      } else if (isThreadNameAsync(threadName)) {
        return ASYNC
      }
      logger.error { "Dispatched event from unknown thread: $threadName" }
      return UNKNOWN
    }
  }
}

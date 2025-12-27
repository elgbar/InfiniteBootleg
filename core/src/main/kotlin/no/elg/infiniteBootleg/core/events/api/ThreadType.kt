package no.elg.infiniteBootleg.core.events.api

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import ktx.async.MainDispatcher
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.exceptions.CalledFromWrongThreadTypeException
import no.elg.infiniteBootleg.core.util.ASYNC_THREAD_NAME
import no.elg.infiniteBootleg.core.util.EVENTS_THREAD_NAME
import no.elg.infiniteBootleg.core.util.SERVER_THREAD_PREFIX
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnBox2d
import no.elg.infiniteBootleg.core.util.launchOnBox2dSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnWorldTicker
import no.elg.infiniteBootleg.core.util.launchOnWorldTickerSuspendable
import no.elg.infiniteBootleg.core.world.ticker.WorldBox2DTicker
import no.elg.infiniteBootleg.core.world.ticker.WorldTicker
import no.elg.infiniteBootleg.core.world.world.World

private val logger = KotlinLogging.logger {}

/**
 * What kind of thread an event originated from.
 */
sealed interface ThreadType {

  /**
   * The event was distracted on the render thread
   */
  data object RENDER : ThreadType {

    /**
     * Run the task on this thread if is the render thread, otherwise launch it on the main render thread
     */
    fun launchOrRun(block: () -> Unit) = if (isCurrentThreadType()) block() else launchOnMain(block)

    /**
     * Run the task on this thread if is the render thread, otherwise launch it on the main render thread
     *
     * @implSpec There is no thread check here because the main dispatcher already does that internally
     **/
    fun launchOrRunSuspended(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend () -> Unit) = launchOnMainSuspendable(start, block = { block() })
  }

  /**
   * The event was distracted from the Box2D physics thread and ashley engine
   */
  data object PHYSICS : ThreadType {

    /**
     * Run the task directly if already on the world's physics (box2d) thread, otherwise launch on the world physics thread.
     *
     * @param world The world reference to the physics (box2d) thread
     *
     * @see no.elg.infiniteBootleg.core.world.box2d.WorldBody
     */
    fun launchOrRun(world: World, block: () -> Unit) = if (isCurrentThreadType()) block() else world.launchOnBox2d(block)

    /**
     * Run the task directly if already on the world's physics (box2d) thread, otherwise launch on the world physics thread.
     *
     * @param world The world reference to the physics (box2d) thread
     *
     * @implSpec There is no thread check here because the [World.box2dCoroutineDispatcher] already does that internally
     *
     * @see no.elg.infiniteBootleg.core.world.box2d.WorldBody
     */
    fun launchOrRunSuspended(world: World, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend () -> Unit) = world.launchOnBox2dSuspendable(start, block = { block() })
  }

  /**
   * The event was dispatched from a world thread, exactly which world is unknown. These kind of events are triggered by the world ticking
   */
  data object TICKER : ThreadType {

    /**
     * Run the task directly if already on the world's ticker thread, otherwise launch on the world ticker thread.
     *
     * @param world The world whose ticker to launch the task on
     *
     * @see WorldTicker
     */
    fun launchOrRun(world: World, block: () -> Unit) = if (isCurrentThreadType()) block() else world.launchOnWorldTicker(block)

    /**
     * Run the task directly if already on the world's ticker thread, otherwise launch on the world ticker thread.
     *
     * @param world The world whose ticker to launch the task on
     *
     * @implSpec There is no thread check here because the [World.worldTickCoroutineDispatcher] already does that internally
     *
     * @see WorldTicker
     */
    fun launchOrRunSuspended(world: World, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend () -> Unit) =
      world.launchOnWorldTickerSuspendable(start, block = { block() })
  }

  /**
   * The event was distracted from the async thread, this is the only event which can be considered truly async
   */
  data object ASYNC : ThreadType {

    /**
     * Launch a new task on the single async thread, even if already on the async thread
     * This will make tasks run in sequence as there is only a single async thread with this dispatcher.
     */
    fun launch(block: () -> Unit) = launchOnAsyncSuspendable { block() }

    /**
     * Launch a new task on default single async thread, even if already on the async thread.
     * This will make tasks run in parallel.
     *
     * This option does not have a `launchOrRun` variant as it defeats the purpose of running on multiple threads at once.
     *
     * @see Dispatchers.Default
     */
    fun launchMultithreaded(block: () -> Unit) = launchOnMultithreadedAsyncSuspendable { block() }

    /**
     * Launch a new task on the single async thread, even if already on the async thread
     * This will make tasks run in sequence as there is only a single async thread with this dispatcher.
     */
    fun launchSuspended(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = launchOnAsyncSuspendable(start, block)

    /**
     * Run the task directly if already on the single async thread, otherwise launch on the single async thread.
     */
    fun launchOrRun(block: () -> Unit) = if (isCurrentThreadType()) block() else launch(block)

    /**
     * Run the task directly if already on the single async thread, otherwise launch on the single async thread.
     */
    suspend fun launchOrRunSuspended(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend () -> Unit) =
      if (isCurrentThreadType()) block() else launchSuspended(start, block = { block() })
  }

  /**
   * The event was distracted from a server thread. These threads are usually created by Netty for handling network events.
   * There is no way to launch tasks on this thread type, use [ASYNC] instead.
   *
   * Tasks on this thread type should be offloaded to [ASYNC] as quickly as po ssible to not block network events.
   */
  data object SERVER : ThreadType

  /**
   * Failed to detect what kind of thread the event was dispatched from.
   *
   * Cannot launch any types of tasks.
   */
  data object UNKNOWN : ThreadType

  fun isCurrentThreadType() = currentThreadType() == this
  fun isDifferentThreadType() = currentThreadType() != this
  fun requireCorrectThreadType(message: (() -> String)? = null) = requireCorrectThreadType(this, message)

  companion object {

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

    private fun isThreadNameServer(threadName: String): Boolean =
      threadName.startsWith(SERVER_THREAD_PREFIX, false) ||
        threadName.startsWith("multiThreadIoEventLoopGroup", false)

    private fun isThreadNameAsync(threadName: String): Boolean =
      ASYNC_THREAD_NAME == threadName ||
        EVENTS_THREAD_NAME == threadName ||
        threadName.startsWith("DefaultDispatcher", false) ||
        threadName.startsWith(ASYNC_THREAD_NAME, false) ||
        threadName.startsWith(EVENTS_THREAD_NAME, false)

    fun currentThreadType(): ThreadType {
      val currentThread = Thread.currentThread()
      val threadName = currentThread.name
      return when {
        currentThread === MainDispatcher.mainThread -> RENDER
        threadName.startsWith(WorldBox2DTicker.BOX2D_TICKER_TAG_PREFIX, false) -> PHYSICS
        threadName.startsWith(WorldTicker.WORLD_TICKER_TAG_PREFIX, false) -> TICKER
        isThreadNameAsync(threadName) -> ASYNC
        isThreadNameServer(threadName) -> SERVER
        else -> {
          logger.error { "Dispatched event from unknown thread: $threadName" }
          UNKNOWN
        }
      }
    }
  }
}

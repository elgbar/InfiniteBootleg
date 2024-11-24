package no.elg.infiniteBootleg.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import ktx.async.newSingleThreadAsyncContext
import no.elg.infiniteBootleg.main.Main
import kotlin.coroutines.CoroutineContext

const val ASYNC_THREAD_NAME = "async"
const val EVENTS_THREAD_NAME = "events"
val singleThreadAsyncDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(ASYNC_THREAD_NAME)
val singleThreadEventDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(EVENTS_THREAD_NAME)

/**
 * Run (cancellable) tasks on other threads
 */
fun launchOnMain(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(MainDispatcher, start = start, block = block)

fun launchOnAsync(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(singleThreadAsyncDispatcher, start = start, block = block)

/**
 * Launch task on the event thread
 */
fun launchOnEvents(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(singleThreadEventDispatcher, start = start, block = block)

/**
 * Run tasks which
 */
fun launchOnMultithreadedAsync(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(Dispatchers.Default, start = start, block = block)

fun launchOnWorldTicker(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(WorldTickCoroutineDispatcher, start = start, block = block)

fun launchOnBox2d(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(Box2DTickCoroutineDispatcher, start = start, block = block)

object WorldTickCoroutineDispatcher : CoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    Main.inst().world?.postWorldTickerRunnable { block.run() }
  }
}

object Box2DTickCoroutineDispatcher : CoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    Main.inst().world?.postBox2dRunnable { block.run() }
  }
}

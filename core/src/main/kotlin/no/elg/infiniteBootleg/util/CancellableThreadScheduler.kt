package no.elg.infiniteBootleg.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import ktx.async.newAsyncContext
import ktx.async.newSingleThreadAsyncContext
import no.elg.infiniteBootleg.main.Main
import kotlin.coroutines.CoroutineContext

const val ASYNC_THREAD_NAME = "async"
val asyncDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(ASYNC_THREAD_NAME)

val async8Dispatcher: AsyncExecutorDispatcher = newAsyncContext(8, ASYNC_THREAD_NAME)

/**
 * Run (cancellable) tasks on other threads
 */
fun launchOnMain(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(MainDispatcher, start = start, block = block)

fun launchOnAsync(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(asyncDispatcher, start = start, block = block)
fun launchOn8Async(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = KtxAsync.launch(async8Dispatcher, start = start, block = block)

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

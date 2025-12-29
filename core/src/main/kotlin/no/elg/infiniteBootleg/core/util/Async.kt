package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import ktx.async.newSingleThreadAsyncContext
import no.elg.infiniteBootleg.core.world.world.World

const val ASYNC_THREAD_NAME = "async"
const val EVENTS_THREAD_NAME = "events"
const val SERVER_THREAD_PREFIX = "netty-server-"
val singleThreadAsyncDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(ASYNC_THREAD_NAME)
val singleThreadEventDispatcher: AsyncExecutorDispatcher = newSingleThreadAsyncContext(EVENTS_THREAD_NAME)

/**
 * Run (cancellable) tasks on other threads
 */
fun launchOnMainSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(MainDispatcher, start = start, block = block)

fun launchOnMain(block: () -> Unit) = Gdx.app.postRunnable(block)

fun launchOnAsyncSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(singleThreadAsyncDispatcher, start = start, block = block)

/**
 * Launch task on the event thread
 */
fun launchOnEventsSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(singleThreadEventDispatcher, start = start, block = block)

/**
 * Run tasks using the default kotlin dispatcher
 */
fun launchOnMultithreadedAsyncSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(Dispatchers.Default, start = start, block = block)

fun World.launchOnWorldTicker(block: () -> Unit) = launchOnWorldTickerSuspendable(block = { block() })
fun World.launchOnWorldTickerSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(worldTickCoroutineDispatcher, start = start, block = block)

fun World.launchOnBox2dSuspendable(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
  KtxAsync.launch(box2dCoroutineDispatcher, start = start, block = block)

fun World.launchOnBox2d(block: () -> Unit) = postBox2dRunnable(block)

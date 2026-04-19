package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongSortedMap
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSortedSet
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkAddedToChunkRendererEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangeRejectedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsyncSuspendable
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.render.WorldRender

typealias SystemTimeMillis = Long

/**
 * Queue and perform validity check on the chunks to render. Delegates the actual rendering to [ChunkRenderer]
 *
 * @author Elg
 */
class QueuedChunkRenderer(private val worldRender: WorldRender) :
  Renderer,
  Disposable {

  private val actualRenderer = ChunkRenderer(worldRender.world)

  // long = SystemTimeMillis
  // map:  ChunkCompactLoc -> SystemTimeMillis
  @GuardedBy("QUEUE_LOCK")
  private val chunkLocToTimeAdded: Long2LongSortedMap = Long2LongLinkedOpenHashMap()

  // When the chunk was added to the queue
  // map: SystemTimeMillis -> Chunk[]
  @GuardedBy("QUEUE_LOCK")
  private val renderTimeAdded: Long2ObjectLinkedOpenHashMap<ObjectLinkedOpenHashSet<TexturedChunk>> = Long2ObjectLinkedOpenHashMap()

  // current rendering chunk
  @GuardedBy("QUEUE_LOCK")
  @Volatile
  private var curr: TexturedChunk? = null
    set(value) = synchronized(QUEUE_LOCK) {
      field = value
    }

  @GuardedBy("QUEUE_LOCK")
  private fun nextChunk(): TexturedChunk? {
    val (time: SystemTimeMillis, chunkTimeBucket: ObjectSortedSet<TexturedChunk>) = renderTimeAdded.firstEntry() ?: let {
      chunksInRenderQueue = 0
      return null
    }
    val chunk = chunkTimeBucket.removeFirst()
    if (chunkTimeBucket.isEmpty()) {
      renderTimeAdded.remove(time)
    }
    chunkLocToTimeAdded.remove(chunk.compactLocation)
    chunksInRenderQueue = chunkLocToTimeAdded.size
    return chunk
  }

  /**
   * Queue rendering of a chunk. If the chunk is already in the queue to be rendered and `prioritize` is `true` then the chunk will be moved to the front of the queue
   *
   * @param chunk The chunk to render
   * @param prioritize If the chunk should be placed at the front of the queue being rendered
   */
  fun queueRendering(chunk: TexturedChunk, prioritize: Boolean) {
    launchOnMultithreadedAsyncSuspendable {
      val pos: ChunkCompactLoc = chunk.compactLocation
      synchronized(QUEUE_LOCK) {
        if (chunk === curr) {
          return@launchOnMultithreadedAsyncSuspendable
        }

        // Time used to prioritize the chunk, a chunk added a while a go should be prioritized over a chunk added just now with prioritize = true to not get stale chunk textures
        val newTime: SystemTimeMillis = System.currentTimeMillis() + if (prioritize) -PRIORITIZATION_ADVANTAGE_ADD_TIME else 0
        val existingTime: SystemTimeMillis = chunkLocToTimeAdded[pos]

        // Remove existing chunk from time bucket when the new time is less than the existing time to push it forward in the queue
        if (existingTime != NOT_IN_COLLECTION && newTime < existingTime) {
          val chunks = renderTimeAdded[existingTime] ?: error("Chunk $chunk is in the queue but not in the renderTimeAdded map")
          chunks.remove(chunk)
          if (chunks.isEmpty()) {
            renderTimeAdded.remove(existingTime)
          }
        }
        // Add the chunk to the queue
        if (existingTime == NOT_IN_COLLECTION || newTime < existingTime) {
          chunkLocToTimeAdded[pos] = newTime
          val timeSet = renderTimeAdded.computeIfAbsent(newTime) { ObjectLinkedOpenHashSet() }
          timeSet.addAndMoveToLast(chunk)
        }
      }
      chunksInRenderQueue = chunkLocToTimeAdded.size
      EventManager.dispatchEvent(ChunkAddedToChunkRendererEvent(chunk.compactLocation, prioritize))
    }
  }

  private fun isNothingToRender(): Boolean {
    // fast return if there is nothing to render
    if (chunkLocToTimeAdded.isEmpty()) {
      chunksInRenderQueue = 0
      return true
    }
    return false
  }

  fun renderMultiple() {
    if (isNothingToRender()) {
      return
    }
    repeat(Settings.chunksToRenderEachFrame) {
      render()
    }
  }

  override fun render() {
    if (isNothingToRender()) {
      return
    }
    // get the first valid chunk to render
    val chunk: TexturedChunk = synchronized(QUEUE_LOCK) {
      do {
        if (isNothingToRender()) {
          return
        }
        val candidateChunk = nextChunk() ?: return
        if (candidateChunk.isInvalid) {
          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(candidateChunk.compactLocation, ChunkTextureChangeRejectedEvent.CHUNK_INVALID_REASON)
          )
          continue
        }
        if (worldRender.isOutOfView(candidateChunk)) {
          candidateChunk.dirty() // Make sure we update texture next time we need to render it

          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(
              candidateChunk.compactLocation,
              ChunkTextureChangeRejectedEvent.CHUNK_OUT_OF_VIEW_REASON
            )
          )
          continue
        }
        if (candidateChunk.chunkColumn.isChunkAboveTopBlock(candidateChunk.chunkY) && candidateChunk.isAllAir) {
          candidateChunk.setAllSkyAir() // Chunks above top block should always be rendered as air

          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(
              candidateChunk.compactLocation,
              ChunkTextureChangeRejectedEvent.CHUNK_ABOVE_TOP_BLOCK_REASON
            )
          )
          continue
        }
        curr = candidateChunk
        return@synchronized candidateChunk
      } while (true)
      @Suppress("KotlinUnreachableCode") // compiler issue
      error("Should never reach here")
    }
    actualRenderer.renderChunk(chunk)
    if (Settings.renderChunkUpdates) {
      EventManager.dispatchEvent(ChunkTextureChangedEvent(chunk.compactLocation))
    }
    curr = null
  }

  override fun dispose() {
    actualRenderer.dispose()
    synchronized(QUEUE_LOCK) { chunkLocToTimeAdded.clear() }
  }

  companion object {
    private val QUEUE_LOCK = Any()

    private const val PRIORITIZATION_ADVANTAGE_ADD_TIME = 1000L
    private const val NOT_IN_COLLECTION = 0L

    var chunksInRenderQueue: Int = 0
  }
}

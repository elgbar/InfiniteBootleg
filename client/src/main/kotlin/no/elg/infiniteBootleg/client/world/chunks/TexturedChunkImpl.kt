package no.elg.infiniteBootleg.client.world.chunks

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.google.errorprone.annotations.concurrent.GuardedBy
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkLightChangedEvent
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.dst2
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsyncSuspendable
import no.elg.infiniteBootleg.core.world.Material.Companion.emitsLight
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.chunks.ViewableChunk
import no.elg.infiniteBootleg.core.world.world.World
import java.util.concurrent.atomic.AtomicBoolean

class TexturedChunkImpl(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord) :
  ChunkImpl(world, chunkX, chunkY),
  TexturedChunk,
  ViewableChunk,
  Chunk {

  @GuardedBy("chunkBody")
  private var fbo: FrameBuffer? = null

  private var isAllSkyAir = false

  /**
   * @return The last tick this chunk's texture was pulled
   */
  override var lastViewedTick: Long = 0

  override val texture: Texture?
    get() {
      if (isAllSkyAir) {
        return null
      }
      synchronized(chunkBody) {
        updateIfDirty()
        return fbo?.colorBufferTexture
      }
    }

  override fun hasTexture(): Boolean = fbo != null

  override fun queueForRendering(prioritize: Boolean) {
    val render = world.render as? ClientWorldRender ?: return
    render.chunkRenderer.queueRendering(this, prioritize)
  }

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] or [texture]
   * called.
   */
  override fun updateIfDirty(): Boolean {
    if (isInvalid || !isDirty) {
      return false
    }
    return super.updateIfDirty().also { wasPrioritize ->
      // Render the world with the changes (but potentially without the light changes)
      queueForRendering(wasPrioritize)
    }
  }

  override fun view() {
    lastViewedTick = world.tick
  }

  override val frameBuffer: FrameBuffer?
    get() {
      if (isDisposed) {
        return null
      }
      synchronized(chunkBody) {
        if (fbo != null) {
          return fbo
        }
        isAllSkyAir = false
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, Chunk.CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE, false)
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        this.fbo = fbo
        return fbo
      }
    }

  private val lightLocs = LongOpenHashSet(0)

  /**Single 1d array stored in a row major order*/
  private val blockLights = Array(Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE) { i ->
    BlockLight(
      this,
      i / Chunk.CHUNK_SIZE,
      i % Chunk.CHUNK_SIZE
    )
  }

  override fun setAllSkyAir() {
    isAllSkyAir = true
    disposeFbo()
  }

  override fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight = blockLights[blockMapIndex(localX, localY)]

  override fun updateAllBlockLights() {
    doUpdateLightMultipleSources(NOT_CHECKING_DISTANCE, checkDistance = false)
  }

  override fun queueLightSource(compactWorldLoc: Long) {
    synchronized(lightLocs) {
      lightLocs.add(compactWorldLoc)
    }
  }

  override fun queueLightSources(compactWorldLocs: WorldCompactLocArray) {
    if (compactWorldLocs.isEmpty()) return
    synchronized(lightLocs) {
      lightLocs.ensureCapacity(lightLocs.size + compactWorldLocs.size)
      for (loc in compactWorldLocs) {
        lightLocs.add(loc)
      }
    }
  }

  override fun flushPendingLightUpdates() {
    if (lightLocs.isEmpty()) return // Fast, unsynchronized return
    if (!Settings.renderLight || !isValid || !world.isLoaded) return
    val sources = synchronized(lightLocs) {
      if (lightLocs.isEmpty()) return
      lightLocs.toLongArray().also { lightLocs.clear() }
    }
    doUpdateLightMultipleSources(sources, checkDistance = true)
  }

  override fun finishLoading() {
    super.finishLoading()
    launchOnAsyncSuspendable {
      delay(200L)
      updateAllBlockLights()
    }
  }

  private fun isNoneWithinDistance(sources: WorldCompactLocArray, worldX: WorldCoord, worldY: WorldCoord): Boolean {
    for ((srcX: WorldCoord, srcY: WorldCoord) in sources) {
      val dstFromChange2blk = dst2(worldX, worldY, srcX, srcY)
      if (dstFromChange2blk <= World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW) {
        return false
      }
    }
    return true
  }

  override fun onRealBlockChange(localX: LocalCoord, localY: LocalCoord, oldBlock: Block?, newBlock: Block?) {
    if (Settings.renderLight) {
      // Optimized to check blockLight last, ok to do it twice (it should be cached for the second lookup anyway(?))
      fun affectedByLight(block: Block?): Boolean =
        block != null && (block.material.emitsLight || (Settings.lightOcclusion && block.material.lightOpacity > 0f && getBlockLight(localX, localY).isLit))

      if (affectedByLight(newBlock) || affectedByLight(oldBlock)) {
        EventManager.dispatchEventAsync(ChunkLightChangedEvent(compactLocation, localX, localY))
      }
    }
  }

  fun doUpdateLightMultipleSources(sources: WorldCompactLocArray, checkDistance: Boolean) {
    if (Settings.renderLight && isValid && world.isLoaded) {
      synchronized(this) {
        // TODO synchronize on something else
        if (!checkDistance) {
          // Safe to cancel when doing a full update
          // Note to self, DO NOT CANCEL when updating from sources,
          // as it might cancel updates to blocks that will not be updated in the next update
          recalculateLightJob?.cancel()
        }
        recalculateLightJob = launchOnMultithreadedAsyncSuspendable {
          doUpdateLightMultipleSources0(sources, checkDistance)
        }
      }
    }
  }

  /**
   * @return if any block was recalculated
   */
  suspend fun doUpdateLightMultipleSources0(sources: WorldCompactLocArray, checkDistance: Boolean) {
    if (Settings.renderLight) {
      val anyRecalculated = AtomicBoolean(false)
      coroutineScope {
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          val worldX = this@TexturedChunkImpl.chunkX.chunkToWorld(localX)
          for (localY in Chunk.CHUNK_SIZE - 1 downTo 0) {
            if (checkDistance && isNoneWithinDistance(
                sources,
                worldX,
                this@TexturedChunkImpl.chunkY.chunkToWorld(localY)
              )
            ) {
              continue
            }
            launch {
              // TODO allow canceling of individual blocks
              val recalculated = getBlockLight(localX, localY).recalculateLighting()
              if (recalculated) {
                anyRecalculated.compareAndSet(false, true)
              }
            }
          }
        }
      }
      if (anyRecalculated.get()) {
        queueForRendering(prioritize = false)
      }
    }
  }

  override fun dispose() {
    super.dispose()
    disposeFbo()
  }

  private fun disposeFbo() {
    synchronized(chunkBody) {
      fbo?.also { oldFbo ->
        launchOnMainSuspendable {
          oldFbo.dispose()
        }
        fbo = null
      }
    }
  }
}

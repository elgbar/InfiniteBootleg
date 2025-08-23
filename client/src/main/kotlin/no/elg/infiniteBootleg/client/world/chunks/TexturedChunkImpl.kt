package no.elg.infiniteBootleg.client.world.chunks

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.google.errorprone.annotations.concurrent.GuardedBy
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.chunks.ViewableChunk
import no.elg.infiniteBootleg.core.world.world.World

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

  override fun setAllSkyAir() {
    isAllSkyAir = true
    disposeFbo()
  }

  override suspend fun doUpdateLightMultipleSources0(sources: WorldCompactLocArray, checkDistance: Boolean): Boolean {
    val doUpdateLightMultipleSources0 = super.doUpdateLightMultipleSources0(sources, checkDistance)
    if (doUpdateLightMultipleSources0) {
      queueForRendering(prioritize = false)
    }
    return doUpdateLightMultipleSources0
  }
}

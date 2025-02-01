package no.elg.infiniteBootleg.world.chunks

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.google.errorprone.annotations.concurrent.GuardedBy
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCompactLocArray
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.world.World

class TexturedChunkImpl(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord) : ChunkImpl(world, chunkX, chunkY), TexturedChunk, ViewableChunk, Chunk {

  @GuardedBy("chunkBody")
  private var fbo: FrameBuffer? = null

  /**
   * @return The last tick this chunk's texture was pulled
   */
  override var lastViewedTick: Long = 0

  override val texture: Texture?
    get() {
      synchronized(chunkBody) {
        if (isDirty) {
          updateIfDirty()
        }
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
  private fun updateIfDirty() {
    if (isInvalid) {
      return
    }
    var wasPrioritize: Boolean
    synchronized(blocks) {
      if (!isDirty || initializing) {
        return
      }
      wasPrioritize = prioritize
      prioritize = false
      isDirty = false

      // test if all the blocks in this chunk has the material air
      isAllAir = true
      outer@ for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
        for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
          val b = blocks[localX][localY]
          if (b != null && b.material !== Material.AIR) {
            isAllAir = false
            break@outer
          }
        }
      }
    }

    // Render the world with the changes (but potentially without the light changes)
    queueForRendering(wasPrioritize)
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
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, Chunk.CHUNK_TEXTURE_SIZE, Chunk.CHUNK_TEXTURE_SIZE, false)
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        this.fbo = fbo
        return fbo
      }
    }

  override var isAllAir: Boolean = false
    get() {
      if (isDirty) {
        updateIfDirty()
      }
      return field
    }

  override fun dispose() {
    super.dispose()
    synchronized(chunkBody) {
      fbo?.also {
        launchOnMain { it.dispose() }
        fbo = null
      }
    }
  }

  override suspend fun doUpdateLightMultipleSources0(sources: WorldCompactLocArray, checkDistance: Boolean): Boolean {
    val doUpdateLightMultipleSources0 = super.doUpdateLightMultipleSources0(sources, checkDistance)
    if (doUpdateLightMultipleSources0) {
      queueForRendering(prioritize = false)
    }
    return doUpdateLightMultipleSources0
  }
}

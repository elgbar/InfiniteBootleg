package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer

interface TexturedChunk : Chunk {

  /**
   * Might cause a call to [updateIfDirty] if the chunk is marked as dirty
   *
   * @return The texture of this chunk
   */
  val texture: Texture?

  /**
   * @return The backing [com.badlogic.gdx.graphics.glutils.FrameBuffer] which holds the texture of this chunk. Will be null if
   * the chunk is disposed, never null otherwise.
   */
  val frameBuffer: FrameBuffer?

  /**
   * Will not update textures
   *
   * @return If this chunk has a texture generated
   */
  fun hasTexture(): Boolean

  fun queueForRendering(prioritize: Boolean)

  /**
   * Mark chunks as air only
   */
  fun setAllSkyAir()
}

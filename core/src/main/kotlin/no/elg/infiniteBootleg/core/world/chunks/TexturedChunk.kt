package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLocArray
import no.elg.infiniteBootleg.core.world.blocks.BlockLight

interface TexturedChunk : Chunk {

  /**
   * Might cause a call to [updateIfDirty] if the chunk is marked as dirty
   *
   * @return The texture of this chunk
   */
  val texture: Texture?

  /**
   * @return The backing [com.badlogic.gdx.graphics.glutils.FrameBuffer] which holds the texture of this chunk. Will be `null` if
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

  /** Update the light of the chunk  */
  fun updateAllBlockLights()

  fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight

  /**
   * Mark chunks as air only
   */
  fun setAllSkyAir()

  /** Add a single world-coordinate light source to the chunk's pending-flush queue. */
  fun queueLightSource(compactWorldLoc: Long)

  /** Add many world-coordinate light sources to the chunk's pending-flush queue. */
  fun queueLightSources(compactWorldLocs: WorldCompactLocArray)

  /**
   * Drain the pending-flush queue and recalculate lighting for the affected blocks.
   * Cheap fast-path when the queue is empty.
   */
  fun flushPendingLightUpdates()
}

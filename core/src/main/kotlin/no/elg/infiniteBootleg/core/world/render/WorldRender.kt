package no.elg.infiniteBootleg.core.world.render

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.api.Resizable
import no.elg.infiniteBootleg.core.api.Updatable
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.world.World

/**
 * @author Elg
 */
interface WorldRender : Updatable, Renderer, Disposable, Resizable {
  /**
   * @param chunk The chunk to check
   * @return `true` if the given chunk is outside the view of the camera
   */
  fun isOutOfView(chunk: Chunk): Boolean

  fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean
  fun isOutOfView(chunkLoc: ChunkCompactLoc): Boolean = isOutOfView(chunkLoc.decompactLocX(), chunkLoc.decompactLocY())

  fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean
  fun isInView(chunkLoc: ChunkCompactLoc): Boolean = isInView(chunkLoc.decompactLocX(), chunkLoc.decompactLocY())

  val world: World
  val chunkLocationsInView: Iterator<Long>

  /**
   * List of chunk columns coordinates that are in view
   */
  val chunkColumnsInView: Set<ChunkCoord>

  companion object {
    const val MIN_ZOOM = 0.25f

    const val MAX_ZOOM = 1.75f

    /**
     * How many chunks around extra should be included.
     *
     *
     * One of the these chunks comes from the need that the player should not see that a chunks
     * loads in.
     */
    const val CHUNKS_IN_VIEW_PADDING_RENDER = 1

    /**
     * How many chunks to the sides extra should be included.
     *
     *
     * This is the for light during twilight to stop light bleeding into the sides of the screen
     * when moving.
     */
    const val CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS = CHUNKS_IN_VIEW_PADDING_RENDER + 1

    /** How much must the player zoom to trigger a skylight reset  */
    const val SKYLIGHT_ZOOM_THRESHOLD = 0.25f
  }
}

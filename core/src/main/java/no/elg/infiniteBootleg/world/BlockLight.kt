package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.render.ChunkRenderer
import kotlin.math.ceil
import kotlin.math.floor

class BlockLight(
  val chunk: Chunk,
  val localX: Int,
  val localY: Int
) {

  /**
   * Whether this block is above the top-most block of the given world y-coordinate
   *
   * It denotes that this is a fully lit block, and thus light can be skipped rendered on it.
   */
  var isSkylight: Boolean
    private set

  /**
   * If this block have any light shining onto it. If not then it should be rendered as a black square
   */
  var isLit: Boolean
    private set

  /**
   * The average brightness of the block
   */
  var averageBrightness: Float
    private set

  /**
   * Brightness, in the range `0..1`, of each sub-cells.
   *
   * Do not modify
   */
  var lightMap: Array<FloatArray> = NO_LIGHTS_LIGHT_MAP
    get() {
      synchronized(this) {
        return field
      }
    }
    private set(value) {
      synchronized(this) {
        field = value
      }
    }

  private val strPos by lazy { CoordUtil.stringifyChunkToWorld(chunk, localX, localY) }

  private val chunkImpl = chunk as ChunkImpl

  init {
    isSkylight = chunk.chunkColumn.isBlockAboveTopBlock(localX, CoordUtil.chunkToWorld(chunk.chunkY, localY), BLOCKS_LIGHT_FLAG)
    isLit = isSkylight
    averageBrightness = if (isSkylight) {
      1f
    } else {
      0f
    }

    lightMap = if (isSkylight) {
      SKYLIGHT_LIGHT_MAP
    } else {
      Array(ChunkRenderer.LIGHT_RESOLUTION) { FloatArray(ChunkRenderer.LIGHT_RESOLUTION) }
    }
  }

  fun recalculateLighting() {
    recalculateLighting(NEVER_CANCEL_UPDATE_ID)
  }

  fun recalculateLighting(updateId: Int) {
    //    System.out.println("Recalculating light for " + getMaterial() + " block " + getWorldX() +
    // "," + getWorldY());
    if (!Settings.renderLight) {
      Main.logger().debug("BL $strPos") { "Not rendering light" }
      return
    }

    fun isCancelled(): Boolean {
      val diff = updateId != chunkImpl.currentUpdateId.get()
      return updateId != NEVER_CANCEL_UPDATE_ID && diff
    }

    val chunkColumn = chunk.chunkColumn
    val worldX: Int = CoordUtil.chunkToWorld(chunk.chunkX, localX)
    val worldY: Int = CoordUtil.chunkToWorld(chunk.chunkY, localY)

    if (chunkColumn.isBlockAboveTopBlock(localX, worldY, BLOCKS_LIGHT_FLAG)) {
      // This block is a skylight, its always lit fully
      isLit = true
      isSkylight = true
      averageBrightness = 1f
      lightMap = SKYLIGHT_LIGHT_MAP

//      Main.logger().trace("BL $strPos") { "Above top-block, setting as skylight" }
      return
    }

    var isLitNext = false
    val tmpLightMap = Array(ChunkRenderer.LIGHT_RESOLUTION) { FloatArray(ChunkRenderer.LIGHT_RESOLUTION) }

    if (isCancelled()) {
      return
    }
    // find light sources around this block
    val blocksAABB = chunk
      .world
      .getBlocksAABB(
        worldX + 0.5f,
        worldY + 0.5f,
        World.LIGHT_SOURCE_LOOK_BLOCKS,
        World.LIGHT_SOURCE_LOOK_BLOCKS,
        true,
        false,
        false,
        ::isCancelled
      )
    if (isCancelled()) {
      return
    }
    // Since we're not creating air blocks above, we will instead just load
    for (offsetWorldX in -floor(World.LIGHT_SOURCE_LOOK_BLOCKS).toInt()..ceil(World.LIGHT_SOURCE_LOOK_BLOCKS).toInt()) {
      val topWorldX = worldX + offsetWorldX
      // add one to get the air block above the top-block
      val topWorldY = 1 + chunk.world.getTopBlockWorldY(topWorldX, BLOCKS_LIGHT_FLAG)
      val topBlock = chunk.world.getBlock(topWorldX, topWorldY, false)
      if (topBlock != null) {
//        Main.logger().debug("BL $strPos") { "topBlock found! (mat: ${topBlock.material}" }
        blocksAABB.add(topBlock)
      }
      if (isCancelled()) {
        return
      }
    }
    for (neighbor in blocksAABB) {
      val neiMat = neighbor.material
      // We assume that if the neighbor is air, it is a topblock
      if (neiMat.isLuminescent || (neiMat.isTransparent && neighbor.chunk.chunkColumn.isBlockAboveTopBlock(neighbor.localX, neighbor.worldY, BLOCKS_LIGHT_FLAG))) {
        if (isCancelled()) {
          return
        }
        isLitNext = true
        for (dx in 0 until ChunkRenderer.LIGHT_RESOLUTION) {
          for (dy in 0 until ChunkRenderer.LIGHT_RESOLUTION) {
            // Calculate distance for each light cell
            val dist = (
              Location.distCubed(
                (
                  worldX + dx.toFloat() / ChunkRenderer.LIGHT_RESOLUTION
                  ).toDouble(),
                (
                  worldY + dy.toFloat() / ChunkRenderer.LIGHT_RESOLUTION
                  ).toDouble(),
                neighbor.worldX + 0.5,
                neighbor.worldY + 0.5
              ) /
                (World.LIGHT_SOURCE_LOOK_BLOCKS * World.LIGHT_SOURCE_LOOK_BLOCKS)
              )
            val old: Float = tmpLightMap[dx][dy]
            val normalizedIntensity = if (dist == 0.0) {
              0f
            } else if (dist > 0) {
              1 - dist.toFloat()
            } else {
              1 + dist.toFloat()
            }

            if (old < normalizedIntensity) {
              tmpLightMap[dx][dy] = normalizedIntensity
            }
          }
        }
      }
    }

    if (isCancelled()) {
      return
    }

    isSkylight = false
    isLit = isLitNext
    if (isLitNext) {
      lightMap = tmpLightMap
      var total = 0.0
      for (x in 0 until ChunkRenderer.LIGHT_RESOLUTION) {
        for (y in 0 until ChunkRenderer.LIGHT_RESOLUTION) {
          total += lightMap[x][y].toDouble()
        }
      }
      averageBrightness = (total / (ChunkRenderer.LIGHT_RESOLUTION * ChunkRenderer.LIGHT_RESOLUTION)).toFloat()
//      Main.logger().debug("BL $strPos") { "Recalculation finished normally - is lit" }
    } else {
      lightMap = NO_LIGHTS_LIGHT_MAP
      averageBrightness = 0f
//      Main.logger().debug("BL $strPos") { "Recalculation finished normally - is not lit" }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BlockLight) return false

    if (chunk != other.chunk) return false
    if (localX != other.localX) return false
    if (localY != other.localY) return false

    return true
  }

  override fun hashCode(): Int {
    var result = chunk.hashCode()
    result = 31 * result + localX
    result = 31 * result + localY
    return result
  }

  companion object {
    const val NEVER_CANCEL_UPDATE_ID = -1
    val SKYLIGHT_LIGHT_MAP: Array<FloatArray> = Array(ChunkRenderer.LIGHT_RESOLUTION) { FloatArray(ChunkRenderer.LIGHT_RESOLUTION) { 1f } }
    val NO_LIGHTS_LIGHT_MAP: Array<FloatArray> = Array(ChunkRenderer.LIGHT_RESOLUTION) { FloatArray(ChunkRenderer.LIGHT_RESOLUTION) { 0f } }
  }
}
